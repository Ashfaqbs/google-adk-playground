package com.example.retail.agents;

import com.example.retail.common.GroqLlmFactory;
import com.example.retail.tools.ReturnTools;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.FunctionTool;

public final class ReturnsAgent {

    private ReturnsAgent() {}

    public static LlmAgent build() {
        return LlmAgent.builder()
                .name("returns_agent")
                .model(GroqLlmFactory.create())
                .description("Handles return eligibility checks, initiating returns, and refund status.")
                .instruction(
                        "Always call checkReturnEligibility before initiateReturn. Never tell a"
                                + " customer a return is approved unless the tool confirms it.")
                .tools(
                        FunctionTool.create(ReturnTools.class, "checkReturnEligibility"),
                        FunctionTool.create(ReturnTools.class, "initiateReturn"),
                        FunctionTool.create(ReturnTools.class, "getRefundStatus"))
                .build();
    }
}
