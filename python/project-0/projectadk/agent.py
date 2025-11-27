from google.adk.agents.llm_agent import Agent 

def get_weather(city: str) -> dict:
    """Returns mock weather details for a given city."""
    return {
        "status": "success",
        "city": city,
        "weather": "Sunny",
        "temperature": "28°C"
    }

def convert_currency(amount: float, from_currency: str, to_currency: str) -> dict:
    """Returns a mock converted currency value."""
    converted_value = amount * 83.0  # dummy conversion logic
    return {
        "status": "success",
        "from": from_currency,
        "to": to_currency,
        "original_amount": amount,
        "converted_amount": converted_value
    }

root_agent = Agent(
    model='gemini-2.5-flash',
    name='root_agent',
    description="Handles weather lookup and currency conversion.",
    instruction="Use the appropriate tool when needed.",
    tools=[get_weather, convert_currency]
)