package com.tejas.distributedmatchingengine.models;

import java.math.BigDecimal;
import java.time.Instant;

// Store the orderIDs instead of the Order objects themselves because the
// objects can change their state after the transaction. orderIDs are
// also lightweight. Because of this, the execution price must be passed
// by the matching thread (lest they have to be retrieved from the Orders
// through the orderIDs causing unnecessary computation).
public record Transaction(long transactionID,
                          Instant executionTime,
                          long restingOrderID,
                          long incomingOrderID,
                          BigDecimal executionPrice,
                          int quantityFilled) { }
