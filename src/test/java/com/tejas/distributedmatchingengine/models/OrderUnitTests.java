package com.tejas.distributedmatchingengine.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderUnitTests {
    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order("user123",
                Type.BUY,
                BigDecimal.valueOf(100.00),
                100);
    }

    @Test
    @DisplayName("Test that order is cancelled.")
    public void orderIsCancelled() {
        order.cancel();
        assertEquals(Status.CANCELLED, order.getStatus());
    }
}
