package com.example.retail.backend;

import java.util.List;

public record Order(
        String orderId, String customerId, String status, String eta, List<OrderItem> items) {}
