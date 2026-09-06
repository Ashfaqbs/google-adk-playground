package com.example.retail.agents;

import com.example.retail.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;

public final class OrderSupportAgentDemo {

    public static void main(String[] args) throws Exception {
        LlmAgent agent = OrderSupportAgent.build();
        RunnerSupport.runScript(
                agent,
                "order_support_agent",
                "What's the status of order ORD-1001?",
                "Track order ORD-1002 for me.");
    }
}
