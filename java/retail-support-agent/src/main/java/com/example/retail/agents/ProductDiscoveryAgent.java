package com.example.retail.agents;

import com.example.retail.common.GroqLlmFactory;
import com.example.retail.tools.CatalogTools;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.FunctionTool;

public final class ProductDiscoveryAgent {

    private ProductDiscoveryAgent() {}

    public static LlmAgent build() {
        return LlmAgent.builder()
                .name("product_discovery_agent")
                .model(GroqLlmFactory.create())
                .description("Helps customers search the catalog and get product recommendations.")
                .instruction(
                        "Use the tools to search the catalog or recommend products. Never invent"
                                + " a product that the tools didn't return.")
                .tools(
                        FunctionTool.create(CatalogTools.class, "searchCatalog"),
                        FunctionTool.create(CatalogTools.class, "getRecommendations"))
                .build();
    }
}
