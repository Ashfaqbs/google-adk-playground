package com.example.retail.agents;

import com.example.retail.common.GroqLlmFactory;
import com.example.retail.tools.OrderTools;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.FunctionTool;

public final class OrderSupportAgent {

    private OrderSupportAgent() {}

    public static LlmAgent build() {
        return LlmAgent.builder()
                .name("order_support_agent")
                .model(GroqLlmFactory.create())
                .description(
                        "Answers questions about order status, delivery ETA, and shipment tracking.")
                .instruction(
                        "You help customers check their order status and tracking. Use the tools"
                                + " provided; never guess an order's status.")
                .tools(
                        FunctionTool.create(OrderTools.class, "getOrderStatus"),
                        FunctionTool.create(OrderTools.class, "getShipmentTracking"))
                .build();
    }
}
