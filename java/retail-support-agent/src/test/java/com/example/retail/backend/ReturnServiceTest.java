package com.example.retail.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReturnServiceTest {

    private final OrderService orderService = new OrderService();
    private final ReturnService returnService = new ReturnService(orderService);

    @Test
    void deliveredOrderItemIsEligible() {
        assertTrue(returnService.isEligible("ORD-1002", "ITEM-2"));
    }

    @Test
    void shippedOrderItemIsNotEligible() {
        assertFalse(returnService.isEligible("ORD-1001", "ITEM-1"));
    }

    @Test
    void initiateReturnCreatesPendingRequestForEligibleItem() {
        ReturnRequest request = returnService.initiateReturn("ORD-1002", "ITEM-2", "Wrong size");

        assertEquals("Pending", request.status());
        assertEquals("ORD-1002", request.orderId());
    }

    @Test
    void initiateReturnThrowsForIneligibleItem() {
        assertThrows(
                IllegalStateException.class,
                () -> returnService.initiateReturn("ORD-1001", "ITEM-1", "Changed mind"));
    }

    @Test
    void getReturnLooksUpByCreatedId() {
        ReturnRequest created = returnService.initiateReturn("ORD-1002", "ITEM-2", "Wrong size");

        assertEquals(created, returnService.getReturn(created.returnId()).orElseThrow());
    }
}
