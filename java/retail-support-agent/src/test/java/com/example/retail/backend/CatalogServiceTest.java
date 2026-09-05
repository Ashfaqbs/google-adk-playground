package com.example.retail.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogServiceTest {

    private final CatalogService service = new CatalogService();

    @Test
    void searchMatchesByNameSubstring() {
        List<Product> results = service.search("hose");

        assertEquals(1, results.size());
        assertEquals("P-1", results.get(0).id());
    }

    @Test
    void searchMatchesByCategory() {
        List<Product> results = service.search("garden");

        assertEquals(3, results.size());
    }

    @Test
    void searchReturnsEmptyForNoMatch() {
        assertTrue(service.search("xyz-nonexistent").isEmpty());
    }

    @Test
    void recommendFiltersByCategory() {
        List<Product> results = service.recommend("tools");

        assertEquals(1, results.size());
        assertEquals("P-4", results.get(0).id());
    }
}
