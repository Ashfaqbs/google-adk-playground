package com.example.retail.tools;

import com.example.retail.backend.OrderService;
import com.example.retail.backend.ReturnRequest;
import com.example.retail.backend.ReturnService;
import com.google.adk.tools.Annotations.Schema;
import java.util.Map;

public final class ReturnTools {

    private static final OrderService ORDER_SERVICE = new OrderService();
    private static final ReturnService RETURN_SERVICE = new ReturnService(ORDER_SERVICE);

    private ReturnTools() {}

    public static Map<String, Object> checkReturnEligibility(
            @Schema(name = "orderId", description = "The order ID") String orderId,
            @Schema(name = "itemId", description = "The item ID within that order") String itemId) {
        boolean eligible = RETURN_SERVICE.isEligible(orderId, itemId);
        return Map.of(
                "status",
                "success",
                "eligible",
                eligible,
                "report",
                eligible
                        ? "Item " + itemId + " on order " + orderId + " is eligible for return."
                        : "Item " + itemId + " on order " + orderId + " is not eligible for return.");
    }

    public static Map<String, Object> initiateReturn(
            @Schema(name = "orderId", description = "The order ID") String orderId,
            @Schema(name = "itemId", description = "The item ID within that order") String itemId,
            @Schema(name = "reason", description = "Why the customer is returning the item")
                    String reason) {
        try {
            ReturnRequest request = RETURN_SERVICE.initiateReturn(orderId, itemId, reason);
            return Map.of(
                    "status",
                    "success",
                    "returnId",
                    request.returnId(),
                    "report",
                    "Return " + request.returnId() + " created for order " + orderId + ".");
        } catch (IllegalStateException e) {
            return Map.of("status", "error", "report", e.getMessage());
        }
    }

    public static Map<String, Object> getRefundStatus(
            @Schema(name = "returnId", description = "The return ID to check") String returnId) {
        return RETURN_SERVICE
                .getReturn(returnId)
                .<Map<String, Object>>map(
                        r ->
                                Map.of(
                                        "status", "success",
                                        "returnId", r.returnId(),
                                        "refundStatus", r.status()))
                .orElse(Map.of("status", "error", "report", "No return found with ID " + returnId));
    }
}
