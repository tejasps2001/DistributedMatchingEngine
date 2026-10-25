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

    // TODO: Since settlement should be atomically done for both the BUY and SELL
    //          together, maybe must settle both the orders in one settle() call
    //          and wrap it with @Transactional (although settling both the orders
    //          in one of the Order objects wouldn't be correct and hence the need
    //          for SettlementService).
    public void settle(UserRepository userRepository,
                       BigDecimal restingOrderPrice) {
        // Start the settlement thread and immediately return to the matching
        // to speed up matching
        orderSettler.execute(() -> {
            // No need to check for presence of non-null value before get()
            // because the non-null check has already happened in the
            // OrderController
            UserWallet wallet = userRepository.findUserByID(userId).get();
            int qtyFilled = quantity - remainingQuantity;
            if (type == Type.BUY) {
                if (status == Status.PARTIALLY_FILLED || status == Status.FILLED) {
                    wallet.addToAvailableShares(qtyFilled);
                    wallet.addToTotalShares(qtyFilled);
                    wallet.tryDeductFromTotalBalance(restingOrderPrice.multiply(
                            BigDecimal.valueOf(qtyFilled)));
                }
                else if (status == Status.CANCELLED) {
                    wallet.addToAvailableBalance(price.multiply(
                            BigDecimal.valueOf(quantity)));
                }
                // If for some reason, the status remains Status.NEW, log it here.
                else {
                    logger.info("Unexpected state while settling a BUY.");
                }
            }
            else {
                if (status == Status.PARTIALLY_FILLED || status == Status.FILLED) {
                    wallet.tryDeductFromTotalShares(qtyFilled);
                    wallet.addToAvailableBalance(restingOrderPrice.multiply(
                            BigDecimal.valueOf(qtyFilled)));
                    wallet.addToTotalBalance(restingOrderPrice.multiply(
                            BigDecimal.valueOf(qtyFilled)));
                }
                else if (status == Status.CANCELLED) {
                    wallet.addToAvailableShares(quantity);
                }
                // If for some reason, the status remains Status.NEW, log it here.
                else {
                    logger.info("Unexpected state while settling a SELL.");
                }
            }
        });
    }
}
