package com.example.retail.tools;

import com.example.retail.backend.OrderService;
import com.google.adk.tools.Annotations.Schema;
import java.util.Map;

public final class OrderTools {

    private static final OrderService SERVICE = new OrderService();

    private OrderTools() {}

    public static Map<String, Object> getOrderStatus(
            @Schema(name = "orderId", description = "The order ID to look up, e.g. ORD-1001")
                    String orderId) {
        return SERVICE
                .getOrder(orderId)
                .<Map<String, Object>>map(
                        order ->
                                Map.of(
                                        "status", "success",
                                        "orderId", order.orderId(),
                                        "orderStatus", order.status(),
                                        "eta", order.eta()))
                .orElse(Map.of("status", "error", "report", "No order found with ID " + orderId));
    }

    public static Map<String, Object> getShipmentTracking(
            @Schema(name = "orderId", description = "The order ID to track, e.g. ORD-1001")
                    String orderId) {
        return SERVICE
                .getOrder(orderId)
                .<Map<String, Object>>map(
                        order ->
                                Map.of(
                                        "status", "success",
                                        "orderId", order.orderId(),
                                        "report",
                                                "Order "
                                                        + order.orderId()
                                                        + " is "
                                                        + order.status()
                                                        + ", expected by "
                                                        + order.eta()
                                                        + "."))
                .orElse(Map.of("status", "error", "report", "No order found with ID " + orderId));
    }
}
