package com.example.retail.backend;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OrderService {

    private final Map<String, Order> orders;

    public OrderService() {
        this.orders =
                Map.of(
                        "ORD-1001",
                        new Order(
                                "ORD-1001",
                                "CUST-1",
                                "Shipped",
                                "2026-09-08",
                                List.of(new OrderItem("ITEM-1", "Garden Hose 50ft", 1))),
                        "ORD-1002",
                        new Order(
                                "ORD-1002",
                                "CUST-1",
                                "Delivered",
                                "2026-08-30",
                                List.of(new OrderItem("ITEM-2", "Pruning Shears", 2))));
    }

    public Optional<Order> getOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public List<Order> getOrdersForCustomer(String customerId) {
        return orders.values().stream()
                .filter(order -> order.customerId().equals(customerId))
                .toList();
    }
}
