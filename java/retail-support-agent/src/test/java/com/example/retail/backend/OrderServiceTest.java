package com.example.retail.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private final OrderService service = new OrderService();

    @Test
    void returnsOrderWhenIdExists() {
        Optional<Order> order = service.getOrder("ORD-1001");

        assertTrue(order.isPresent());
        assertEquals("Shipped", order.get().status());
    }

    @Test
    void returnsEmptyWhenIdDoesNotExist() {
        assertTrue(service.getOrder("ORD-9999").isEmpty());
    }

    @Test
    void returnsAllOrdersForCustomer() {
        List<Order> orders = service.getOrdersForCustomer("CUST-1");

        assertEquals(2, orders.size());
    }
}
