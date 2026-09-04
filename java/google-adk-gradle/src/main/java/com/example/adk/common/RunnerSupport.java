package com.example.adk.common;

import com.google.adk.agents.BaseAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;

/** Small helper shared by the demo packages: runs a fixed script of user turns against an agent. */
public final class RunnerSupport {

    private static final String USER_ID = "demo-user";

    private RunnerSupport() {}

    public static void runScript(BaseAgent agent, String appName, String... turns) {
        InMemoryRunner runner = new InMemoryRunner(agent);
        Session session = runner.sessionService().createSession(appName, USER_ID).blockingGet();

        for (String turn : turns) {
            System.out.println("\nYou > " + turn);
            Content userMsg = Content.fromParts(Part.fromText(turn));
            Flowable<Event> events = runner.runAsync(USER_ID, session.id(), userMsg);
            System.out.print("Agent > ");
            events.blockingForEach(event -> System.out.println(event.stringifyContent()));
        }
    }
}
