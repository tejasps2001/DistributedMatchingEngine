package com.tejas.distributedmatchingengine.models;

import jakarta.validation.constraints.NotNull;
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
    // Use an atomic variable so that the incoming sequence of orders
    // is preserved correctly in the orderID
    private static final AtomicLong nextId = new AtomicLong(1);

    private long orderId;
    @NotNull(message = "UserID must not be null")
    private String userId;
    @NotNull(message = "Order type must not be null")
    private Type type;
    @NotNull(message = "Order price must not be null")
    private BigDecimal price; // Price automatically means the per-share price
    @NotNull(message = "Order quantity must not be null")
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
}
