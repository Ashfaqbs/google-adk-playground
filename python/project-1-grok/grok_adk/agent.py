from google.adk.agents import Agent
from google.adk.models.lite_llm import LiteLlm

def get_weather_info(city: str) -> dict:
    """Returns mock weather details for a given city."""
    return {
        "status": "success",
        "city": city,
        "weather": "Cloudy",
        "temperature": "24°C"
    }


def get_city_population(city: str) -> dict:
    """Returns mock population details for a given city."""
    return {
        "status": "success",
        "city": city,
        "population": "8.2 million"
    }


root_agent = Agent(
    name="groq_city_agent",
    model=LiteLlm(model="groq/llama-3.3-70b-versatile"),
    description="Provides weather and population information for cities.",
    instruction=(
        "Use only the tools provided to answer city-related questions."
    ),
    tools=[get_weather_info, get_city_population]
)