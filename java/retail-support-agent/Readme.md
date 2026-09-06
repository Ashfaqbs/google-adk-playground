# retail-support-agent

A retail customer-support agent built on Google ADK Java: one orchestrator routing to four
specialist sub-agents. Implements the design in
`docs/superpowers/specs/2026-09-04-retail-support-agent-design.md` (core flow only — guardrails,
human-in-the-loop confirmation, escalation, and the eval harness are a follow-up).

## Setup

```
cd java/retail-support-agent
cp .env.example .env
# edit .env and set GROQ_API_KEY
```

The FAQ/Policy agent needs its MCP server running first:

```
cd python/project-4-retail-policy-mcp
pip install -r requirements.txt
python policy_mcp_server.py
```

## Agents

| Agent | Handles | Tools |
|---|---|---|
| `order_support_agent` | Order status, shipment tracking | `getOrderStatus`, `getShipmentTracking` |
| `returns_agent` | Return eligibility, initiating returns, refund status | `checkReturnEligibility`, `initiateReturn`, `getRefundStatus` |
| `product_discovery_agent` | Catalog search, recommendations | `searchCatalog`, `getRecommendations` |
| `faq_policy_agent` | Shipping/returns/warranty/hours policy | MCP toolset → `search_policy_docs` |
| `retail_support_orchestrator` | Routes every turn to the right specialist above | `subAgents` only |

## Running

```
gradle runOrderDemo       # order support in isolation
gradle runReturnsDemo     # returns in isolation
gradle runCatalogDemo     # product discovery in isolation
gradle runFaqDemo         # FAQ/policy in isolation (needs the MCP server running)
gradle runOrchestrator    # full multi-turn conversation across all four specialists (needs the MCP server running)
gradle test               # backend service + tool unit tests
```

If the policy MCP server isn't running, `runOrchestrator` does not fail immediately at startup:
`OrchestratorAgent.build()` succeeds and the first three turns (order status, returns, catalog)
complete normally with real answers. The failure only happens on turn 4 (the policy question),
once the orchestrator routes to `faq_policy_agent` and it tries to load its MCP toolset — at that
point the process exits with the "Could not reach the policy MCP server..." message and a
non-zero exit code. So an MCP outage takes down only the FAQ/Policy path; the other three
specialists are unaffected until a turn actually needs them.

## Backend data

Everything runs against small in-memory services seeded with two orders (`ORD-1001` shipped,
`ORD-1002` delivered) and five products across garden/outdoor/tools categories — see
`src/main/java/com/example/retail/backend/`.
