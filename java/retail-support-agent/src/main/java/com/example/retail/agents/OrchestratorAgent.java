package com.example.retail.agents;

import com.example.retail.common.GroqLlmFactory;
import com.google.adk.agents.LlmAgent;
import com.google.common.collect.ImmutableList;

public final class OrchestratorAgent {

    private OrchestratorAgent() {}

    public static LlmAgent build() {
        return LlmAgent.builder()
                .name("retail_support_orchestrator")
                .model(GroqLlmFactory.create())
                .description("Routes retail support conversations to the right specialist.")
                .instruction(
                        "You do not answer questions yourself. Delegate every message to the"
                                + " sub-agent whose description best matches it: order status/tracking"
                                + " to order_support_agent, returns/refunds to returns_agent, product"
                                + " search/recommendations to product_discovery_agent, and policy"
                                + " questions (shipping, returns window, warranty, store hours) to"
                                + " faq_policy_agent.")
                .subAgents(
                        ImmutableList.of(
                                OrderSupportAgent.build(),
                                ReturnsAgent.build(),
                                ProductDiscoveryAgent.build(),
                                FaqPolicyAgent.build()))
                .build();
    }
}
