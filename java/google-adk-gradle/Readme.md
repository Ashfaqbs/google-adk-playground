# google-adk-gradle

A Gradle-based companion to `java/google-adk-0` (which uses Maven). Each numbered package
under `src/main/java/com/example/adk/` demonstrates one ADK Java feature in isolation, and all
of them run against **Groq** (an OpenAI-compatible endpoint) via the `google-adk-langchain4j`
bridge, since ADK talks to Gemini natively and needs that bridge for any other provider.

## Prerequisites

- JDK 17 (tested with Zulu 17.0.14 at `C:\openjdk\zuluJDK17.0.14`)
- Gradle (tested with 9.2.1, already on PATH)
- A Groq API key from https://console.groq.com

## Setup

```
cd java/google-adk-gradle
cp .env.example .env
# edit .env and set GROQ_API_KEY
```

`.env` is git-ignored. The build reads it automatically for every `runP*` task -
no need to export environment variables by hand.

> **Security note:** if you pasted real Groq API keys into a chat or shared history to get
> this project built, treat those keys as compromised and rotate them at
> https://console.groq.com/keys. Never commit `.env`.

## Packages

| # | Package | ADK feature |
|---|---------|-------------|
| 0 | `p0_basic_agent` | `LlmAgent` with plain Java-method function tools (`FunctionTool`) |
| 1 | `p1_groq_llm` | Swapping the model backend - `LangChain4j` + `OpenAiChatModel` pointed at Groq's base URL |
| 2 | `p2_multi_agent` | Multi-agent orchestration - a router `LlmAgent` with `.subAgents(...)`, delegates via `transfer_to_agent` |
| 3 | `p3_session_state` | Session state - one agent writes via `.outputKey(...)`, the next reads it back with `Instruction.Provider` + `ctx.state()` |
| 4 | `p4_structured_output` | Structured output - `.outputSchema(Schema...)` forces a strict JSON reply |
| 5 | `p5_streaming` | Streaming - `RunConfig.StreamingMode.SSE` emits partial `Event`s as tokens arrive |
| 6 | `p6_mcp_tools` | MCP tool integration - `McpToolset` over `StreamableHttpServerParameters` calls tools from an external MCP server |

`common/GroqLlmFactory` is the shared piece: it builds one `LangChain4j` model (with both a
`ChatModel` and a `StreamingChatModel`, since streaming needs the latter) reading
`GROQ_API_KEY` / `GROQ_MODEL` from the environment.

## Running

```
gradle runP0     # basic agent + tools
gradle runP1     # Groq-backed plain agent
gradle runP2     # multi-agent routing
gradle runP3     # session state pipeline
gradle runP4     # structured JSON output
gradle runP5     # streaming
gradle runAll    # p0-p5 back to back
```

`runP6` needs an MCP server running first (it talks to the existing fastmcp demo server in
this repo):

```
cd ../../python/project-3-mcp-client
pip install fastmcp
python mcp_server.py
```

Then, in another terminal:

```
gradle runP6
```

## Notes

- Default model is `openai/gpt-oss-120b`; override per-run by setting `GROQ_MODEL` in `.env`
  (e.g. `meta-llama/llama-4-scout-17b-16e-instruct`).
- Each demo runs a small fixed script of user turns (not an interactive prompt) so it's a
  deterministic, one-shot test of that feature.
