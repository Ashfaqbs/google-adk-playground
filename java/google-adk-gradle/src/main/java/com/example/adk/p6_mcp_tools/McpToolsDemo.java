package com.example.adk.p6_mcp_tools;

import com.example.adk.common.GroqLlmFactory;
import com.example.adk.common.RunnerSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.JsonBaseModel;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.StreamableHttpServerParameters;
import java.time.Duration;

/**
 * ADK feature: MCP tool integration. McpToolset connects to an external MCP server and exposes
 * every tool it advertises to the agent, without the agent code knowing how those tools work.
 *
 * Prerequisite: start the MCP server from this repo in another terminal first:
 *   cd python/project-3-mcp-client
 *   pip install fastmcp
 *   python mcp_server.py
 * It serves streamable HTTP at http://127.0.0.1:9000/mcp with one tool: concat_strings(a, b).
 */
public final class McpToolsDemo {

    private static final String NAME = "mcp_tools_agent";
    private static final String MCP_SERVER_URL = "http://127.0.0.1:9000/mcp";

    public static LlmAgent buildAgent() {
        StreamableHttpServerParameters connectionParams =
                StreamableHttpServerParameters.builder()
                        .url(MCP_SERVER_URL)
                        .timeout(Duration.ofSeconds(30))
                        .readTimeout(Duration.ofMinutes(5))
                        .build();

        ObjectMapper objectMapper = JsonBaseModel.getMapper();
        McpToolset mcpToolset = new McpToolset(connectionParams, objectMapper);

        return LlmAgent.builder()
                .name(NAME)
                .model(GroqLlmFactory.create())
                .description("Agent that uses tools exposed by an external MCP server.")
                .instruction(
                        "Use the tools available to you to satisfy the user's request. Do not"
                                + " attempt the task yourself if a tool can do it.")
                .tools(mcpToolset)
                .build();
    }

    public static void main(String[] args) throws Exception {
        try {
            RunnerSupport.runScript(
                    buildAgent(), NAME, "Concatenate the strings 'Hello, ' and 'ADK!'.");
        } catch (Exception e) {
            System.err.println(
                    "Could not reach the MCP server at "
                            + MCP_SERVER_URL
                            + ". Start it first: cd python/project-3-mcp-client && python"
                            + " mcp_server.py");
            throw e;
        }
    }
}
