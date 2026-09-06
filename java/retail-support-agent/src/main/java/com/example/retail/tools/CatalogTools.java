package com.example.retail.tools;

import com.example.retail.backend.CatalogService;
import com.example.retail.backend.Product;
import com.google.adk.tools.Annotations.Schema;
import java.util.List;
import java.util.Map;

public final class CatalogTools {

    private static final CatalogService SERVICE = new CatalogService();

    private CatalogTools() {}

    public static Map<String, Object> searchCatalog(
            @Schema(name = "query", description = "Product name or category keyword to search for")
                    String query) {
        List<Product> results = SERVICE.search(query);
        if (results.isEmpty()) {
            return Map.of("status", "error", "report", "No products matched '" + query + "'.");
        }
        return Map.of("status", "success", "items", results.stream().map(Product::name).toList());
    }

    public static Map<String, Object> getRecommendations(
            @Schema(
                            name = "category",
                            description = "Product category to recommend from, e.g. garden")
                    String category) {
        List<Product> results = SERVICE.recommend(category);
        if (results.isEmpty()) {
            return Map.of("status", "error", "report", "No recommendations for '" + category + "'.");
        }
        return Map.of("status", "success", "items", results.stream().map(Product::name).toList());
    }
}
