package com.example.retail.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CatalogToolsTest {

    @Test
    void searchCatalogReturnsSuccessForMatch() {
        Map<String, Object> result = CatalogTools.searchCatalog("garden");

        assertEquals("success", result.get("status"));
    }

    @Test
    void searchCatalogReturnsSuccessWithEmptyItemsForNoMatch() {
        Map<String, Object> result = CatalogTools.searchCatalog("xyz-nonexistent");

        assertEquals("success", result.get("status"));
        assertTrue(((List<?>) result.get("items")).isEmpty());
    }

    @Test
    void getRecommendationsReturnsSuccessForKnownCategory() {
        Map<String, Object> result = CatalogTools.getRecommendations("tools");

        assertEquals("success", result.get("status"));
    }
}
