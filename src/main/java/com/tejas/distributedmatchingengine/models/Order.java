package com.tejas.distributedmatchingengine.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode
public class Order {
    private static long nextId = 1L;

    private long orderId;
    private String userId;
    private Type type;
    private BigDecimal price; // Single order so no mention of type. Price automatically means the per-share price
    private int quantity;
    private int remainingQuantity;
    private LocalDateTime timestamp;
    private Status status;

    public Order(String userId,
                 Type type,
                 BigDecimal price,
                 int quantity) {
        this.type = type;
        this.price = price;
        this.quantity = this.remainingQuantity = quantity;
        this.timestamp = LocalDateTime.now();
        this.status = Status.NEW;
    }

    public String tostring() {
        return String.format("%s order %d placed by %s \n" +
                        "for %d shares @ ₹ %f. \n\n" +

                        "%d shares filled.\n" +
                        "%d shares not filled.",
                type, orderId, userId, quantity,
                price, quantity, remainingQuantity);
    }

    public void cancel() {
        status = Status.CANCELLED;
    }
}
