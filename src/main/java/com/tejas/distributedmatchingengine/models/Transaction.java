package com.tejas.distributedmatchingengine.models;

import java.math.BigDecimal;
import java.util.UUID;

public record Transaction(String transactionID,
                          Order restingOrder,
                          Order incomingOrder,
                          int quantityFilled,
                          BigDecimal executionPrice) {
    // Could have retrieved executionPrice from restingOrder, but it will become unnecessarily coupled
    public Transaction(Order restingOrder,
                       Order incomingOrder,
                       int quantityFilled) {
        this(UUID.randomUUID().toString(),
                restingOrder,
                incomingOrder,
                quantityFilled,
                restingOrder.getPrice());
    }

//    public static Transaction create(Order resting, Order incoming, int quantity, Instant timestamp) {
//        return new Transaction(
//                UUID.randomUUID().toString(),
//                resting,
//                incoming,
//                quantity,
//                resting.getPrice(), // Business rule encapsulated in factory
//                timestamp
//        );
//    }

    public String toString() {
        return String.format("Transaction %s: %d shares @ ₹%s between %s and %s",
                transactionID,
                quantityFilled,
                executionPrice.toPlainString(),
                restingOrder.getOrderId(),
                incomingOrder.getOrderId());
    }
}
