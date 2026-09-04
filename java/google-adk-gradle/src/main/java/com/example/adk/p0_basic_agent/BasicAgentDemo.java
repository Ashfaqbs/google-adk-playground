package com.example.adk.p0_basic_agent;

import com.example.adk.common.GroqLlmFactory;
import com.example.adk.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * ADK feature: a single LlmAgent equipped with plain Java-method function tools.
 * The model decides when to call getWeather/getCurrentTime based on the user's question.
 */
public final class BasicAgentDemo {

    private static final String NAME = "basic_weather_time_agent";

    public static LlmAgent buildAgent() {
        return LlmAgent.builder()
                .name(NAME)
                .model(GroqLlmFactory.create())
                .description("Agent that answers questions about the time and weather in a city.")
                .instruction(
                        "You are a helpful agent who can answer user questions about the time and"
                                + " weather in a city. Use the provided tools instead of guessing.")
                .tools(
                        FunctionTool.create(BasicAgentDemo.class, "getCurrentTime"),
                        FunctionTool.create(BasicAgentDemo.class, "getWeather"))
                .build();
    }

    public static Map<String, String> getCurrentTime(
            @Schema(name = "city", description = "The city to get the current time for") String city) {
        String normalized = city.trim().toLowerCase().replace(' ', '_');
        return ZoneId.getAvailableZoneIds().stream()
                .filter(zid -> zid.toLowerCase().endsWith("/" + normalized))
                .findFirst()
                .map(
                        zid ->
                                Map.of(
                                        "status",
                                        "success",
                                        "report",
                                        "The current time in "
                                                + city
                                                + " is "
                                                + ZonedDateTime.now(ZoneId.of(zid))
                                                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                                                + "."))
                .orElse(Map.of("status", "error", "report", "No timezone data for " + city + "."));
    }

    public static Map<String, String> getWeather(
            @Schema(name = "city", description = "The city to get the weather for") String city) {
        if (city.equalsIgnoreCase("new york")) {
            return Map.of(
                    "status",
                    "success",
                    "report",
                    "The weather in New York is sunny, 25 degrees Celsius (77 F).");
        }
        return Map.of("status", "error", "report", "Weather data for " + city + " is not available.");
    }

    public static void main(String[] args) throws Exception {
        RunnerSupport.runScript(
                buildAgent(),
                NAME,
                "What's the weather like in New York?",
                "What time is it in Tokyo?");
    }
}
