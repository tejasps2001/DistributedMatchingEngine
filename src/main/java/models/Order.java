package models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
public class Order {
    private enum Side {BUY, SELL;}

    private enum Status {NEW, PARTIALLY_FILLED, FILLED}

    private long orderId;
    private String userId;
    private Side type;
    private BigDecimal price; // Single order so no mention of type. price automatically means the per share price
    private int quantity;
    private int remainingQuantity;
    private long timestamp;
    private Status status;

    public String tostring() {
        return String.format("%s order %d placed by %s \n" +
                        "for %d shares @ ₹ %f. \n\n" +

                        "%d shares filled.\n" +
                        "%d shares not filled.",
                type, orderId, userId, quantity,
                price, quantity, remainingQuantity);
    }
}
