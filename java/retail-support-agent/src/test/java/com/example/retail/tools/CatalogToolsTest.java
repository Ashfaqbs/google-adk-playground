package com.example.retail.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CatalogToolsTest {

    @Test
    void searchCatalogReturnsSuccessForMatch() {
        Map<String, Object> result = CatalogTools.searchCatalog("garden");

        assertEquals("success", result.get("status"));
    }

    @Test
    void searchCatalogReturnsErrorForNoMatch() {
        Map<String, Object> result = CatalogTools.searchCatalog("xyz-nonexistent");

        assertEquals("error", result.get("status"));
    }

    @Test
    void getRecommendationsReturnsSuccessForKnownCategory() {
        Map<String, Object> result = CatalogTools.getRecommendations("tools");

        assertEquals("success", result.get("status"));
    }
}
