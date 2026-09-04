# Retail Customer Support Agent — Design

Status: approved (design phase) — no implementation yet
Owner: Ashfaqbs

## Overview

A conversational customer-support agent for a retail/e-commerce brand, built on Google ADK
(Java), following the router-plus-specialists pattern used in Google's own reference sample
(`adk-samples/contrib` — Cymbal Home & Garden customer-service agent). This spec captures the
agent topology, tools, state, and cross-cutting concerns; it intentionally contains no code —
each numbered package in `java/google-adk-gradle` already proves out the underlying ADK
mechanism this design leans on.

## Goals

- Handle the full breadth of a retail support conversation: order status/tracking, returns &
  refunds, product discovery/recommendations, policy FAQ, and escalation to a human.
- Reuse state across turns so the user is never asked to repeat themselves within a session.
- Keep every domain's logic, tools, and guardrails isolated so one specialist can change without
  destabilizing the others.
- Make responses UI-renderable (structured data) where a UI would want a card, not prose.

## Non-goals (for this design)

- Multi-channel delivery (SMS/WhatsApp/voice) — web chat only for v1.
- Authentication/identity provider integration — assume `customer:id` is already resolved before
  the agent session starts.
- Choice of vector store / RAG backend for policy docs — left as an implementation detail.

## Architecture

**Pattern:** one router (orchestrator) agent with no domain tools of its own, plus four domain
specialist sub-agents. The orchestrator classifies intent each turn and delegates via
`transfer_to_agent`; control returns to it after each specialist turn, so routing is re-evaluated
every turn based on the latest session state (mirrors the behavior already verified in
`java/google-adk-gradle`'s `p2_multi_agent` package).

```
                    ┌─────────────────────┐
   user turn  ───▶  │   Orchestrator       │
                    │  (router + escalate) │
                    └──────────┬───────────┘
                 transfer_to_agent (per turn)
        ┌───────────┬──────────┼──────────┬───────────┐
        ▼           ▼          ▼          ▼           │
   Order Support  Returns   Product     FAQ / Policy   │
   Agent          Agent     Discovery   Agent          │
                             Agent                     │
                                                        ▼
                                         escalate_to_human (orchestrator only)
```

Escalation is not a 5th conversational specialist. A human handoff is a side effect the
orchestrator decides on — a specialist failing repeatedly, the router unable to classify intent,
or detected user frustration — not something the user "talks to."

## Agents

| Agent | Responsibility | Owns tools? |
|---|---|---|
| Orchestrator (root) | Classify intent, delegate, decide on escalation | Only `escalate_to_human` |
| Order Support | Order status, shipment tracking | Yes |
| Returns & Refunds | Return eligibility, initiate return, refund status | Yes |
| Product Discovery | Catalog search, recommendations/upsell | Yes |
| FAQ / Policy | Shipping policy, warranty, store hours | Yes (via MCP toolset) |

## Tools

| Agent | Tool | Notes |
|---|---|---|
| Order Support | `get_order_status(order_id \| customer_id)` | |
| Order Support | `get_shipment_tracking(order_id)` | |
| Returns | `check_return_eligibility(order_id, item_id)` | Must run before `initiate_return` — enforced by guardrail, not just prompt |
| Returns | `initiate_return(order_id, item_id, reason)` | High-risk — subject to human-in-the-loop confirmation |
| Returns | `get_refund_status(return_id)` | |
| Product Discovery | `search_catalog(query, filters)` | |
| Product Discovery | `get_recommendations(customer_id, context)` | |
| FAQ / Policy | MCP toolset → `search_policy_docs(query)` | Same mechanism as `p6_mcp_tools`, pointed at a real retrieval server instead of the demo `concat_strings` server |
| Orchestrator | `escalate_to_human(reason, transcript_summary)` | Only the orchestrator calls this — specialists signal "unresolved" back to it in conversation, they don't escalate directly |

Every tool returns a `{status, ...}` shape (success/error), the same convention already used in
`p0_basic_agent` — failures are data the model reasons about, not exceptions that kill the turn.

## Session state

Populated once per session (after auth, outside the agent):

- `customer:id`, `customer:name`, `customer:tier`

Written by specialists as the conversation progresses, so follow-ups like "cancel that" or "show
me more like that" resolve without re-asking:

- `context:last_order_id` (Order Support)
- `context:pending_return_id` (Returns)
- `context:last_search` (Product Discovery)

The orchestrator's routing instruction reads this state dynamically (`Instruction.Provider`,
proven in `p3_session_state`) — e.g. prefer routing back to Returns if
`context:pending_return_id` is set and the user's message is ambiguous.

## Structured output

Anything a UI would render as a card uses `outputSchema` (proven in `p4_structured_output`):

- Order status → `{orderId, status, eta, items[]}`
- Return status → `{returnId, status, refundAmount, eta}`
- Product results → `{items: [{id, name, price, imageUrl}]}`

FAQ/Policy answers stay free text plus a citations list — policy answers are prose-heavy and
don't benefit from rigid schemas.

## Streaming

Every user-facing reply streams token-by-token (`RunConfig.StreamingMode.SSE`, proven in
`p5_streaming`) — required for chat UX responsiveness.

## Guardrails & human-in-the-loop

Cross-cutting concerns live in the callback/plugin layer, not in per-agent prompts:

- `before_tool_callback`: reject `initiate_return` unless `check_return_eligibility` already ran
  in the current turn/session — don't rely on the model always checking first.
- `before_model_callback`: strip/block PII before it reaches logs; block the model from promising
  refund amounts or discounts outside a policy table.
- Human-in-the-loop confirmation (native to ADK Java 1.0+) on high-risk actions — large refunds,
  order cancellation — the tool does not fire until the user explicitly confirms.
- Escalation triggers (evaluated by the orchestrator): repeated tool failure, repeated re-routing
  without resolution, or detected frustration in the user's message.

## Error handling

- Tool failure (backend timeout/down) → structured `{status: "error", ...}` → model apologizes
  and retries once or escalates; never hallucinates a result.
- Model/provider outage → one retry, then fall back to a secondary model.
- Guardrail block → safe fallback message returned to the user, incident logged; the turn is
  never silently dropped.

## Testing & evaluation

- Each specialist gets its own canned golden-path transcript, same shape as the numbered demo
  packages (`p0`–`p6`) already in `java/google-adk-gradle`.
- ADK's built-in eval framework (`google-adk-dev`, already a dependency) runs fixed transcripts
  against expected tool calls/outcomes — the regression net for future prompt/tool changes.
- Success criteria: correct routing rate on a held-out intent set, escalation fires on the
  designed trigger cases, structured outputs are always valid against their schema, zero PII in
  logs.

## Relationship to the existing playground

This design is a direct extension of `java/google-adk-gradle`'s numbered packages — it doesn't
introduce a new ADK mechanism, it composes the ones already proven there:

| Design element | Proven by |
|---|---|
| Function tools with `{status, ...}` results | `p0_basic_agent` |
| Router + sub-agent delegation | `p2_multi_agent` |
| Cross-turn session state | `p3_session_state` |
| `outputSchema` structured responses | `p4_structured_output` |
| Streaming replies | `p5_streaming` |
| MCP-backed tool (FAQ retrieval) | `p6_mcp_tools` |

Not yet proven in the playground and net-new for implementation: guardrail/plugin callbacks,
human-in-the-loop confirmation, and the eval-harness usage.

## Open questions (for the implementation plan, not this design)

- Retrieval backend for policy docs (pgvector, Vertex AI RAG Engine, or another vector store).
- Where session state persists in production (Firestore-backed `SessionService` vs. in-memory).
- Ticketing system integration target for `escalate_to_human`.
