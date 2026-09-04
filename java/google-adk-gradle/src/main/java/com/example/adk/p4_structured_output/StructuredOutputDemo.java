package com.example.adk.p4_structured_output;

import com.example.adk.common.GroqLlmFactory;
import com.example.adk.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;
import com.google.common.collect.ImmutableMap;
import com.google.genai.types.Schema;

/**
 * ADK feature: structured output. outputSchema forces the model's final reply to be JSON
 * matching the given schema instead of free-form prose, so downstream code can parse it directly.
 */
public final class StructuredOutputDemo {

    private static final String NAME = "structured_forecast_agent";

    public static LlmAgent buildAgent() {
        Schema forecastSchema =
                Schema.builder()
                        .type("OBJECT")
                        .properties(
                                ImmutableMap.of(
                                        "city", Schema.builder().type("STRING").build(),
                                        "temperatureCelsius", Schema.builder().type("INTEGER").build(),
                                        "condition", Schema.builder().type("STRING").build()))
                        .build();

        return LlmAgent.builder()
                .name(NAME)
                .model(GroqLlmFactory.create())
                .description("Returns a made-up weather forecast as strict JSON.")
                .instruction(
                        "Invent a plausible weather forecast for the requested city and return ONLY"
                                + " JSON matching the schema: city, temperatureCelsius, condition.")
                .outputSchema(forecastSchema)
                .build();
    }

    public static void main(String[] args) throws Exception {
        RunnerSupport.runScript(buildAgent(), NAME, "Give me the forecast for Paris.");
    }
}
