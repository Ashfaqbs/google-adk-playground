package com.example.retail.backend;

import java.util.List;

public final class CatalogService {

    private final List<Product> products =
            List.of(
                    new Product("P-1", "Garden Hose 50ft", 29.99, "garden"),
                    new Product("P-2", "Pruning Shears", 14.99, "garden"),
                    new Product("P-3", "Patio Chair", 89.99, "outdoor"),
                    new Product("P-4", "Cordless Drill", 59.99, "tools"),
                    new Product("P-5", "Watering Can", 12.99, "garden"));

    public List<Product> search(String query) {
        String needle = query.toLowerCase();
        return products.stream()
                .filter(
                        p ->
                                p.name().toLowerCase().contains(needle)
                                        || p.category().toLowerCase().contains(needle))
                .toList();
    }

    public List<Product> recommend(String category) {
        return products.stream().filter(p -> p.category().equalsIgnoreCase(category)).toList();
    }
}
