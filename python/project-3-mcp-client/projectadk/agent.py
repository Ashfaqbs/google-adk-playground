from google.adk.agents import Agent
from google.adk.tools.mcp_tool import McpToolset, StreamableHTTPConnectionParams
# StdioConnectionParams for stdio
# SseConnectionParams for SSE

def get_string_stats(text: str) -> dict:
    """Return basic statistics about a string."""
    words = text.split()
    return {
        "status": "success",
        "text": text,
        "length": len(text),
        "word_count": len(words),
        "is_all_upper": text.isupper(),
        "is_all_lower": text.islower(),
    }


mcp_tools = McpToolset(
    connection_params=StreamableHTTPConnectionParams(
        # This must match the FastMCP server URL in server.py
        url="http://127.0.0.1:9000/mcp",
        timeout=30.0,
        sse_read_timeout=60.0,
        terminate_on_close=False,
    ),
    # Optional: if we  want to filter which tools to expose, we can use tool_filter
    # tool_filter=["concat_strings", "repeat_string"],
)


root_agent = Agent(
    model="gemini-2.5-flash",
    name="fastmcp_demo_agent",
    description=(
        "Demo agent that calls a local FastMCP MCP server running at "
        "http://127.0.0.1:9000/mcp for math and string utilities. "
        "It can add numbers, concatenate strings, and repeat strings via MCP tools."
    ),
    instruction=(
        "You are a helpful assistant with access to both local Python tools and MCP tools.\n\n"
        "TOOLS OVERVIEW:\n"
        "- Use get_string_stats(text) when the user asks about properties of a string, "
        "such as its length, number of words, or whether it is all upper/lower case.\n\n"
        "- Use the MCP tools (from the MCPToolset) for string operations:\n"
        "  * 'concat_strings(a, b)' to join two strings directly.\n"
        "  * 'repeat_string(text, times)' to repeat text a given number of times.\n\n"
        "BEHAVIOR:\n"
        "- When the user asks to combine strings (e.g., 'concat \"foo\" and \"bar\"'), "
        "use 'concat_strings'.\n"
        "- When the user asks to repeat or echo a string N times, use 'repeat_string'.\n"
        "- If the user asks something unrelated to these tools, answer directly without calling tools."
    ),
    tools=[
        get_string_stats,  # local Python tool
        mcp_tools,         # remote MCP toolset (FastMCP server)
    ],
)