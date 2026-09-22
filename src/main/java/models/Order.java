package models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode
public class Order {
    private enum Side {BUY, SELL;}
    private enum Status {NEW, PARTIALLY_FILLED, FILLED}

    private static long nextId = 1L;

    private long orderId;
    private String userId;
    private Side type;
    private BigDecimal price; // Single order so no mention of type. Price automatically means the per-share price
    private int quantity;
    private int remainingQuantity;
    private LocalDateTime timestamp;
    private Status status;

    public Order(String userId,
                 Side type,
                 BigDecimal price,
                 int quantity) {
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.remainingQuantity = 0;
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
}
