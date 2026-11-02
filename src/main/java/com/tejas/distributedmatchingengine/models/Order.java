package com.tejas.distributedmatchingengine.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
@EqualsAndHashCode
public class Order {
    // Use an atomic variable so that the incoming sequence is preserved
    // correctly by the orderID
    private static final AtomicLong nextId = new AtomicLong(1);

    private long orderId;
    private String userId;
    private Type type;
    private BigDecimal price; // Single order so no mention of type. Price automatically means the per-share price
    private int quantity;
    private int remainingQuantity;
    private Instant timestamp;
    private Status status;

    public Order(String userId,
                 Type type,
                 BigDecimal price,
                 int quantity) {
        this.orderId = nextId.getAndIncrement();
        this.userId = userId;
        this.type = type;
        this.timestamp = Instant.now();
        this.status = Status.NEW;

        if (price.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Price must be a non-zero " +
                    "positive value.");
        else
            this.price = price;

        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity must be a non-zero " +
                    "positive integer.");
        else
            this.quantity = this.remainingQuantity = quantity;
    }

    public String toString() {
        return String.format("""
                        %s order %d placed by %s\s
                        for %d shares @ ₹ %s.\s
                        
                        %d filled.""",
                type, orderId, userId, quantity,
                price.toPlainString(), quantity - remainingQuantity);
    }

    public String toCompactString() {
        return String.format("%s order of %d @ ₹%s",
                type, quantity, price.toPlainString());
    }

    public Status cancel() {
        if (status == Status.NEW) {
            status = Status.CANCELLED;
        } else if (status == Status.PARTIALLY_FILLED) {
            return status;
        }
        return status;
    }
}
