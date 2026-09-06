from fastmcp import FastMCP

mcp = FastMCP("PolicyDocs")

POLICIES = {
    "shipping": "Standard shipping takes 3-5 business days. Express shipping takes 1-2 business days.",
    "returns": "Items can be returned within 30 days of delivery for a full refund, in original condition.",
    "warranty": "All power tools carry a 1-year manufacturer warranty covering defects.",
    "hours": "Stores are open Monday-Saturday 9am-8pm, and Sunday 10am-6pm.",
}


@mcp.tool
def search_policy_docs(query: str) -> dict:
    """Search store policy documents (shipping, returns, warranty, hours) for a keyword."""
    query_lower = query.lower()
    matches = {
        topic: text
        for topic, text in POLICIES.items()
        if query_lower in topic or query_lower in text.lower()
    }
    if not matches:
        return {"status": "error", "report": f"No policy information found for '{query}'."}
    return {"status": "success", "matches": matches}


if __name__ == "__main__":
    mcp.run(
        transport="http",
        host="127.0.0.1",
        port=9001,
        path="/mcp",
    )
