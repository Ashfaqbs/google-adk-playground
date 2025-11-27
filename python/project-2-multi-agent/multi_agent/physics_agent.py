from math import factorial as _factorial
from math import gcd as _gcd
from google.adk.agents import LlmAgent


def calculate_force(mass: float, acceleration: float) -> dict:
    """Compute force using F = m * a."""
    return {
        "status": "success",
        "operation": "force",
        "mass": mass,
        "acceleration": acceleration,
        "result": mass * acceleration,
    }


def kinetic_energy(mass: float, velocity: float) -> dict:
    """Compute kinetic energy KE = 0.5 * m * v^2."""
    return {
        "status": "success",
        "operation": "kinetic_energy",
        "mass": mass,
        "velocity": velocity,
        "result": 0.5 * mass * (velocity ** 2),
    }


def potential_energy(mass: float, height: float, gravity: float = 9.8) -> dict:
    """Compute gravitational potential energy PE = m * g * h."""
    return {
        "status": "success",
        "operation": "potential_energy",
        "mass": mass,
        "height": height,
        "gravity": gravity,
        "result": mass * gravity * height,
    }

physics_agent = LlmAgent(
    name="PhysicsAgent",
    model="gemini-2.0-flash",
    description="Performs basic physics calculations.",
    instruction=(
        "Use the correct physics tool whenever the user asks a physics-related question. "
        "Give short explanations when needed."
    ),
    tools=[calculate_force, kinetic_energy, potential_energy],
)
