package com.example.adk.p2_multi_agent;

import com.example.adk.common.GroqLlmFactory;
import com.example.adk.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;
import com.google.common.collect.ImmutableList;

/**
 * ADK feature: multi-agent orchestration. A root agent has no domain knowledge itself; it reads
 * each sub-agent's description and transfers the conversation to whichever one fits the question.
 */
public final class MultiAgentDemo {

    private static final String NAME = "root_router_agent";

    public static LlmAgent buildAgent() {
        LlmAgent geographyAgent =
                LlmAgent.builder()
                        .name("geography_agent")
                        .model(GroqLlmFactory.create())
                        .description("Answers questions about countries, capitals, and geography.")
                        .instruction("You are a geography expert. Answer geography questions concisely.")
                        .build();

        LlmAgent physicsAgent =
                LlmAgent.builder()
                        .name("physics_agent")
                        .model(GroqLlmFactory.create())
                        .description("Answers questions about physics laws, formulas, and concepts.")
                        .instruction("You are a physics expert. Answer physics questions concisely.")
                        .build();

        return LlmAgent.builder()
                .name(NAME)
                .model(GroqLlmFactory.create())
                .description("Router agent that delegates to a geography or physics specialist.")
                .instruction(
                        "You do not answer questions yourself. Delegate every question to the"
                                + " sub-agent whose description best matches it.")
                .subAgents(ImmutableList.of(geographyAgent, physicsAgent))
                .build();
    }

    public static void main(String[] args) throws Exception {
        RunnerSupport.runScript(
                buildAgent(),
                NAME,
                "What is the capital of France?",
                "What is Newton's second law of motion?");
    }
}
