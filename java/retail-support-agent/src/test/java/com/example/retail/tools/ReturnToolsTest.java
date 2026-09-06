package com.example.retail.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ReturnToolsTest {

    @Test
    void checkReturnEligibilityReportsTrueForDeliveredItem() {
        Map<String, Object> result = ReturnTools.checkReturnEligibility("ORD-1002", "ITEM-2");

        assertEquals(true, result.get("eligible"));
    }

    @Test
    void initiateReturnFailsForIneligibleItem() {
        Map<String, Object> result = ReturnTools.initiateReturn("ORD-1001", "ITEM-1", "Changed mind");

        assertEquals("error", result.get("status"));
    }

    @Test
    void initiateReturnSucceedsForEligibleItem() {
        Map<String, Object> result = ReturnTools.initiateReturn("ORD-1002", "ITEM-2", "Wrong size");

        assertEquals("success", result.get("status"));
    }
}
