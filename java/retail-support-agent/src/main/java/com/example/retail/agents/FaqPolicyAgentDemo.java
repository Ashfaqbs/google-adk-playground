package com.example.retail.agents;

import com.example.retail.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;

public final class FaqPolicyAgentDemo {

    private static final String MCP_SERVER_URL = "http://127.0.0.1:9001/mcp";

    public static void main(String[] args) throws Exception {
        try {
            LlmAgent agent = FaqPolicyAgent.build();
            RunnerSupport.runScript(
                    agent, "faq_policy_agent", "What's your return policy?", "What are your store hours?");
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
