from google.adk.agents import LlmAgent
from google.adk.tools import agent_tool

from .physics_agent import physics_agent
from .geography_agent import geography_agent

coordinator = LlmAgent(
    name="Coordinator",
    model="gemini-2.0-flash",
    description="Delegates requests to PhysicsAgent or GeographyAgent.",
    instruction=(
        "Your role is to route the user's request to the correct specialist agent.\n\n"
        "- Use PhysicsAgent for questions involving forces, energy, motion, or physical formulas.\n"
        "- Use GeographyAgent for questions involving countries, capitals, populations, continents, or related facts.\n\n"
        "If a request does not fall under any specialist area, respond directly using your own language model.\n"
        "Always choose the tool that best fits the intent. Responses should be concise unless the user asks for more detail."
    ),
    tools=[
        agent_tool.AgentTool(agent=physics_agent),
        agent_tool.AgentTool(agent=geography_agent)
        # Optional hierarchical delegation
    ],
    sub_agents=[physics_agent, geography_agent],
)

root_agent = coordinator
