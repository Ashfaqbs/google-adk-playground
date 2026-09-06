package com.example.retail.agents;

import com.example.retail.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;

public final class ReturnsAgentDemo {

    public static void main(String[] args) throws Exception {
        LlmAgent agent = ReturnsAgent.build();
        RunnerSupport.runScript(
                agent,
                "returns_agent",
                "I want to return item ITEM-2 from order ORD-1002, it's the wrong size.",
                "Can I return item ITEM-1 from order ORD-1001 too? I just don't like it.");
    }
}
