package com.tejas.distributedmatchingengine.models;

import com.tejas.distributedmatchingengine.repositories.UserRepository;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Getter
@Setter
@EqualsAndHashCode
public class Order {
    private static long nextId = 1L;
    private static Executor orderSettler = Executors.newFixedThreadPool(8);
    private static Logger logger = Logger.getLogger(Order.class.getName());

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
        this.orderId = nextId;
        nextId++;
        this.userId = userId;
        this.type = type;
        this.price = price;
        this.quantity = this.remainingQuantity = quantity;
        this.timestamp = Instant.now();
        this.status = Status.NEW;
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

    // Return 1 on a successful cancellation; 0 when order is already filled
    public Status cancel() {
        if (status == Status.NEW) {
            status = Status.CANCELLED;
        }
        // Have a redundant else if statement even for the below case because
        // modifying/cancelling partially filled orders will be implemented
        // in future milestones.
        else if (status == Status.FILLED) {
            return status;
        }
        return status;
    }
}
