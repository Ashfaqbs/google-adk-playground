from google.adk.agents import LlmAgent


def country_capital(country: str) -> dict:
    """Return capital of a country (mock data)."""
    sample = {
        "India": "New Delhi",
        "USA": "Washington, D.C.",
        "Japan": "Tokyo",
    }
    return {
        "status": "success",
        "task": "capital_lookup",
        "country": country,
        "capital": sample.get(country, "Unknown"),
    }


def country_population(country: str) -> dict:
    """Return population details of a country (mock data)."""
    sample = {
        "India": "1.4 Billion",
        "USA": "331 Million",
        "Japan": "125 Million",
    }
    return {
        "status": "success",
        "task": "population_lookup",
        "country": country,
        "population": sample.get(country, "Unknown"),
    }


def continent_of_country(country: str) -> dict:
    """Return continent for a given country."""
    sample = {
        "India": "Asia",
        "USA": "North America",
        "Japan": "Asia",
    }
    return {
        "status": "success",
        "task": "continent_lookup",
        "country": country,
        "continent": sample.get(country, "Unknown"),
    }


def nearest_ocean(country: str) -> dict:
    """Return the nearest ocean for a country."""
    sample = {
        "India": "Indian Ocean",
        "USA": "Pacific / Atlantic",
        "Japan": "Pacific Ocean",
    }
    return {
        "status": "success",
        "task": "nearest_ocean",
        "country": country,
        "ocean": sample.get(country, "Unknown"),
    }



geography_agent = LlmAgent(
    name="GeographyAgent",
    model="gemini-2.0-flash",
    description="Provides world facts such as capitals, continents, and populations.",
    instruction=(
        "Use the correct geography tool when asked about countries or global facts."
    ),
    tools=[country_capital, country_population, continent_of_country, nearest_ocean],
)
