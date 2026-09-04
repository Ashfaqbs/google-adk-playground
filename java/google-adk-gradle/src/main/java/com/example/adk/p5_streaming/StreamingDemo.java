package com.example.adk.p5_streaming;

import com.example.adk.common.GroqLlmFactory;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;

/**
 * ADK feature: streaming. Setting RunConfig.StreamingMode.SSE makes the runner emit partial
 * Events as tokens arrive instead of one Event for the whole reply.
 */
public final class StreamingDemo {

    private static final String NAME = "streaming_story_agent";
    private static final String USER_ID = "demo-user";

    public static LlmAgent buildAgent() {
        return LlmAgent.builder()
                .name(NAME)
                .model(GroqLlmFactory.create())
                .description("Tells a short story, streamed as it is generated.")
                .instruction("Write a 4-sentence story about a robot learning to paint.")
                .build();
    }

    public static void main(String[] args) throws Exception {
        InMemoryRunner runner = new InMemoryRunner(buildAgent());
        Session session = runner.sessionService().createSession(NAME, USER_ID).blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("Tell me the story."));
        Flowable<Event> events =
                runner.runAsync(
                        USER_ID,
                        session.id(),
                        userMsg,
                        RunConfig.builder().setStreamingMode(RunConfig.StreamingMode.SSE).build());

        System.out.println("Streaming chunks as they arrive:\n");
        int[] chunkCount = {0};
        events.blockingForEach(
                event -> {
                    chunkCount[0]++;
                    System.out.println("[chunk " + chunkCount[0] + "] " + event.stringifyContent());
                });
        System.out.println("\nReceived " + chunkCount[0] + " chunk(s) total.");
    }
}
