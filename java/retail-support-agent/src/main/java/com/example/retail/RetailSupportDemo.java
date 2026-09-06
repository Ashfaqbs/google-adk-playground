package com.example.retail;

import com.example.retail.agents.OrchestratorAgent;
import com.example.retail.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;

public final class RetailSupportDemo {

    public static void main(String[] args) throws Exception {
        LlmAgent orchestrator = OrchestratorAgent.build();
        RunnerSupport.runScript(
                orchestrator,
                "retail_support_orchestrator",
                "What's the status of order ORD-1001?",
                "I want to return item ITEM-2 from order ORD-1002, it's the wrong size.",
                "Do you have any garden products in stock?",
                "What's your return policy?");
    }
}
