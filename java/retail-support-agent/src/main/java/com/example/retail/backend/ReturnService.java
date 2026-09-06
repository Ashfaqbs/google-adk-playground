package com.example.retail.backend;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReturnService {

    private final OrderService orderService;
    private final Map<String, ReturnRequest> returns = new ConcurrentHashMap<>();
    private final AtomicInteger nextReturnId = new AtomicInteger(1);

    public ReturnService(OrderService orderService) {
        this.orderService = orderService;
    }

    public boolean isEligible(String orderId, String itemId) {
        return orderService
                .getOrder(orderId)
                .map(
                        order ->
                                "Delivered".equals(order.status())
                                        && order.items().stream()
                                                .anyMatch(item -> item.itemId().equals(itemId)))
                .orElse(false);
    }

    public ReturnRequest initiateReturn(String orderId, String itemId, String reason) {
        if (!isEligible(orderId, itemId)) {
            throw new IllegalStateException(
                    "Order " + orderId + " item " + itemId + " is not eligible for return");
        }
        String returnId = "RET-" + nextReturnId.getAndIncrement();
        ReturnRequest request = new ReturnRequest(returnId, orderId, itemId, reason, "Pending", 0.0);
        returns.put(returnId, request);
        return request;
    }

    public Optional<ReturnRequest> getReturn(String returnId) {
        return Optional.ofNullable(returns.get(returnId));
    }
}
