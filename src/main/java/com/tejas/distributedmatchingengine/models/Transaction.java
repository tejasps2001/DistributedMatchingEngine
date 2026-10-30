package com.tejas.distributedmatchingengine.models;

import java.math.BigDecimal;
import java.time.Instant;

public record Transaction(long transactionID,
                          long restingOrderID,
                          long incomingOrderID,
                          int quantityFilled,
                          BigDecimal executionPrice,
                          Instant executionTime) {
    // Could have retrieved executionPrice from restingOrder, but it will become
    // unnecessarily coupled.
    // Not coupling anymore since directly passing the execution price to the
    // constructor
    // Store the orderIDs instead of the Order objects themselves because the
    // objects can change their state after the transaction anyway. orderIDs
    // are lightweight. Because of this, the execution price must be passed
    // by the matching thread (lest they have to be retrieved from the Orders
    // through the orderIDs causing unnecessary computation.
    public Transaction(long transactionID,
                       Instant executionTime,
                       long restingOrderID,
                       long incomingOrderID,
                       BigDecimal executionPrice,
                       int qtyFulfilled) {
        this(transactionID,
                restingOrderID,
                incomingOrderID,
                qtyFulfilled,
                executionPrice,
                executionTime);
    }

    @Override
    public String toString() {
        return String.format("Transaction %s: %d shares @ ₹%s between %d and %d",
                transactionID,
                quantityFilled,
                executionPrice.toPlainString(),
                restingOrderID,
                incomingOrderID);
    }
}
