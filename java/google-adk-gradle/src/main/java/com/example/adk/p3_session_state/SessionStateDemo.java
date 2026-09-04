package com.example.adk.p3_session_state;

import com.example.adk.common.GroqLlmFactory;
import com.example.adk.common.RunnerSupport;
import com.google.adk.agents.Instruction;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.common.collect.ImmutableList;
import io.reactivex.rxjava3.core.Single;

/**
 * ADK feature: session state shared between agents. The first agent writes its answer into
 * session state via outputKey; the second agent reads that state back inside a dynamic
 * instruction (Instruction.Provider) instead of receiving it as chat history.
 */
public final class SessionStateDemo {

    private static final String NAME = "session_state_pipeline";
    private static final String NAME_KEY = "extracted:user_name";

    public static SequentialAgent buildAgent() {
        LlmAgent extractor =
                LlmAgent.builder()
                        .name("name_extractor")
                        .model(GroqLlmFactory.create())
                        .description("Extracts the user's first name from their message.")
                        .instruction(
                                "Extract only the person's first name from the user's message. Reply"
                                        + " with just the name, nothing else.")
                        .outputKey(NAME_KEY)
                        .build();

        LlmAgent greeter =
                LlmAgent.builder()
                        .name("greeter")
                        .model(GroqLlmFactory.create())
                        .description("Greets the user by the name found in session state.")
                        .instruction(
                                new Instruction.Provider(
                                        ctx ->
                                                Single.just(
                                                        "Greet the user warmly by name in one sentence. Their name,"
                                                                + " read from session state, is: "
                                                                + ctx.state().get(NAME_KEY))))
                        .build();

        return SequentialAgent.builder()
                .name(NAME)
                .subAgents(ImmutableList.of(extractor, greeter))
                .build();
    }

    public static void main(String[] args) throws Exception {
        RunnerSupport.runScript(buildAgent(), NAME, "Hi, my name is Priya and I love Java.");
    }
}
