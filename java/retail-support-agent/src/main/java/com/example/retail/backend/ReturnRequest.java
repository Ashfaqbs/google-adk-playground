package com.example.retail.backend;

public record ReturnRequest(
        String returnId, String orderId, String itemId, String reason, String status, double refundAmount) {}
