package com.example.retail.agents;

import com.example.retail.common.GroqLlmFactory;
import com.google.adk.JsonBaseModel;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.StreamableHttpServerParameters;
import java.time.Duration;

public final class FaqPolicyAgent {

    private static final String MCP_SERVER_URL = "http://127.0.0.1:9001/mcp";

    private FaqPolicyAgent() {}

    public static LlmAgent build() {
        StreamableHttpServerParameters connectionParams =
                StreamableHttpServerParameters.builder()
                        .url(MCP_SERVER_URL)
                        .timeout(Duration.ofSeconds(30))
                        .readTimeout(Duration.ofMinutes(5))
                        .build();

        McpToolset mcpToolset = new McpToolset(connectionParams, JsonBaseModel.getMapper());

        return LlmAgent.builder()
                .name("faq_policy_agent")
                .model(GroqLlmFactory.create())
                .description(
                        "Answers questions about shipping, returns, warranty, and store hours policy.")
                .instruction(
                        "Use the search_policy_docs tool to answer policy questions. Only answer"
                                + " from what the tool returns; do not invent policy details.")
                .tools(mcpToolset)
                .build();
    }
}
