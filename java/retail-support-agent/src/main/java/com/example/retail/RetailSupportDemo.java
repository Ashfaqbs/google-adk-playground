package com.example.retail;

import com.example.retail.agents.OrchestratorAgent;
import com.example.retail.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;

public final class RetailSupportDemo {

    private static final String MCP_SERVER_URL = "http://127.0.0.1:9001/mcp";

    public static void main(String[] args) throws Exception {
        try {
            LlmAgent orchestrator = OrchestratorAgent.build();
            RunnerSupport.runScript(
                    orchestrator,
                    "retail_support_orchestrator",
                    "What's the status of order ORD-1001?",
                    "I want to return item ITEM-2 from order ORD-1002, it's the wrong size.",
                    "Do you have any garden products in stock?",
                    "What's your return policy?");
        } catch (Exception e) {
            System.err.println(
                    "Could not reach the policy MCP server at "
                            + MCP_SERVER_URL
                            + ". Start it first: cd python/project-4-retail-policy-mcp && pip install -r"
                            + " requirements.txt && python policy_mcp_server.py");
            throw e;
        }
    }
}
