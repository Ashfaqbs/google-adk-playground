package com.example.retail.agents;

import com.example.retail.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;

public final class ProductDiscoveryAgentDemo {

    public static void main(String[] args) throws Exception {
        LlmAgent agent = ProductDiscoveryAgent.build();
        RunnerSupport.runScript(
                agent,
                "product_discovery_agent",
                "Do you have any garden products in stock?",
                "What tools would you recommend?");
    }
}
