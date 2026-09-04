package com.example.retail.common;

import com.google.adk.models.langchain4j.LangChain4j;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.time.Duration;

public final class GroqLlmFactory {

    private static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1";
    private static final String DEFAULT_MODEL = "openai/gpt-oss-120b";

    private GroqLlmFactory() {}

    public static LangChain4j create() {
        return create(System.getenv().getOrDefault("GROQ_MODEL", DEFAULT_MODEL));
    }

    public static LangChain4j create(String modelName) {
        String apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GROQ_API_KEY is not set. Copy .env.example to .env in java/retail-support-agent"
                            + " and fill in your key.");
        }

        OpenAiChatModel chatModel =
                OpenAiChatModel.builder()
                        .baseUrl(GROQ_BASE_URL)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .timeout(Duration.ofSeconds(60))
                        .build();

        // Streaming needs a StreamingChatModel too - the adapter throws otherwise.
        OpenAiStreamingChatModel streamingChatModel =
                OpenAiStreamingChatModel.builder()
                        .baseUrl(GROQ_BASE_URL)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .timeout(Duration.ofSeconds(60))
                        .build();

        return LangChain4j.builder()
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .modelName(modelName)
                .build();
    }
}
