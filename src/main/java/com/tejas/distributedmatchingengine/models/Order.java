package com.tejas.distributedmatchingengine.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@EqualsAndHashCode
public class Order {
    // Use an atomic variable so that the incoming sequence of orders
    // is preserved correctly in the orderID
    private static final AtomicLong nextId = new AtomicLong(1);

    private final long orderId;
    private final String userId;
    private final Type type;
    private final BigDecimal price; // Price automatically means the per-share
    private final int quantity;
    @Setter
    private int remainingQuantity;
    private final Instant timestamp; // Order creation time
    @Setter
    private Status status;

    public Order(String userId,
                 Type type,
                 BigDecimal price,
                 int quantity) {
        this.orderId = nextId.getAndIncrement();
        this.userId = Objects.requireNonNull(userId,
                "UserID must not be null");
        this.type = Objects.requireNonNull(type,
                "Order type must not be null");
        this.timestamp = Instant.now();
        this.status = Status.NEW;

        Objects.requireNonNull(price, "Order price must not be null");
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
}
