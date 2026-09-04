package com.example.adk.p1_groq_llm;

import com.example.adk.common.GroqLlmFactory;
import com.example.adk.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;

/**
 * ADK feature: swapping the model backend. ADK talks to Gemini natively; this shows how
 * {@code google-adk-langchain4j} lets an LlmAgent run against any OpenAI-compatible endpoint
 * (here, Groq) by pointing an {@code OpenAiChatModel} at Groq's base URL and API key.
 *
 * Set GROQ_API_KEY (and optionally GROQ_MODEL) in java/google-adk-gradle/.env before running.
 */
public final class GroqAgentDemo {

    private static final String NAME = "groq_llm_demo_agent";

    public static LlmAgent buildAgent() {
        return LlmAgent.builder()
                .name(NAME)
                .model(GroqLlmFactory.create())
                .description("Plain conversational agent running entirely on Groq.")
                .instruction("You are a concise, helpful assistant. Answer in at most 3 sentences.")
                .build();
    }

    public static void main(String[] args) throws Exception {
        RunnerSupport.runScript(
                buildAgent(), NAME, "Explain the importance of fast language models.");
    }
}
