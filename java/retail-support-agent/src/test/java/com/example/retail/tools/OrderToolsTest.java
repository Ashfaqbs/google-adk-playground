package com.example.retail.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderToolsTest {

    @Test
    void getOrderStatusReturnsSuccessForKnownOrder() {
        Map<String, Object> result = OrderTools.getOrderStatus("ORD-1001");

        assertEquals("success", result.get("status"));
        assertEquals("Shipped", result.get("orderStatus"));
    }

    @Test
    void getOrderStatusReturnsErrorForUnknownOrder() {
        Map<String, Object> result = OrderTools.getOrderStatus("ORD-0000");

        assertEquals("error", result.get("status"));
    }

    @Test
    void getShipmentTrackingReturnsSuccessForKnownOrder() {
        Map<String, Object> result = OrderTools.getShipmentTracking("ORD-1002");

        assertEquals("success", result.get("status"));
    }
}
