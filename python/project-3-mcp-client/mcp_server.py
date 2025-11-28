from fastmcp import FastMCP

mcp = FastMCP("Demo")

@mcp.tool
def concat_strings(a: str, b: str) -> str:
    """Concatenate two strings directly."""
    print(a + b)
    return a + b

if __name__ == "__main__":
    mcp.run(
        transport="http",
        host="127.0.0.1",
        port=9000,    
        path="/mcp",  # Streamable HTTP endpoint
    )
