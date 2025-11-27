package com.example;

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.InMemorySessionService;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.adk.events.Event;
import io.reactivex.rxjava3.core.Flowable;


public class SimpleTest {
    public static void main(String[] args) {
        LlmAgent llmAgent = LlmAgent.builder()
                .name("TravelPlanningAgent")
                // Only Anthropic Claude Family and Gemini Family are supported in the Java Version in 0.2.0
                .model("gemini-2.0-flash")
                // A concise summary of the agent's overall purpose. This becomes crucial later
                // when other agents need to decide whether to delegate tasks to this agent.
                .description("A helpful assistant to brainstorm and plan travel itineraries.")
                // Detailed guidance for the LLM on how to behave, its persona, its goals,
                // and specifically how and when to utilize its assigned `tools`.
                .instruction("""
				You are a friendly and enthusiastic travel planning assistant.
				Your goal is to help users by providing travel suggestions, and creating potential itineraries.
				Engage with the user to understand their interests and preferences for their trip.
				Provide informative and inspiring suggestions for destinations, activities, and local cuisine.
				""")
                .build();

        String appName = "MarvelTravel";
        String userId = "SpiderMan";
        InMemorySessionService sessionService = new InMemorySessionService();
        Session session = sessionService.createSession(appName, userId).blockingGet();

        Runner runner = new Runner(llmAgent, appName, null, sessionService);

        Content userMsg = Content.fromParts(Part.fromText("I want to go Asgard!"));
        Flowable<Event> events = runner.runAsync(userId, session.id(), userMsg);
        events.blockingForEach(event -> System.out.println(event.stringifyContent()));
        }

}