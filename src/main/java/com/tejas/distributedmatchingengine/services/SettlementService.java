package com.tejas.distributedmatchingengine.services;

import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.Type;
import com.tejas.distributedmatchingengine.models.UserWallet;
import com.tejas.distributedmatchingengine.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Service
public class SettlementService {
    private final UserRepository userRepository;

    private final static Logger logger =
            Logger.getLogger(SettlementService.class.getName());
    private final Executor settlementHandler = Executors.newFixedThreadPool(
            8);

    public SettlementService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // TODO: @Transactional
    public void settle(Order restingOrder, Order incomingOrder, int qtyFulfilled) {
        settlementHandler.execute(() -> {
            if(restingOrder.getType() == Type.BUY) {
                settleBuy(restingOrder, restingOrder.getPrice(), qtyFulfilled);
                settleSell(incomingOrder, restingOrder.getPrice(), qtyFulfilled);
            }
            else {
                settleBuy(incomingOrder, restingOrder.getPrice(), qtyFulfilled);
                settleSell(restingOrder, restingOrder.getPrice(), qtyFulfilled);
            }
        });
    }

    public void settleBuy(Order buyOrder,
                          BigDecimal executionPrice,
                          int qtyFulfilled) {
        UserWallet wallet = userRepository.findUserByID(buyOrder.getUserId())
                .get();

        if (buyOrder.getStatus() == Status.PARTIALLY_FILLED ||
                buyOrder.getStatus() == Status.FILLED) {
            wallet.addToAvailableShares(qtyFulfilled);
            wallet.addToTotalShares(qtyFulfilled);
            // Ignore the boolean return value since we know for sure that
            // the funds are present
            wallet.tryDeductFromTotalBalance(buyOrder.getPrice().multiply(
                    BigDecimal.valueOf(qtyFulfilled)));

            // Refund for an aggressive buy cross-edge
            if(buyOrder.getPrice().compareTo(executionPrice) > 0) {
                BigDecimal buyOrderValue = buyOrder.getPrice().multiply(
                        BigDecimal.valueOf(qtyFulfilled));
                BigDecimal executionValue = executionPrice.multiply(
                        BigDecimal.valueOf(qtyFulfilled));
                BigDecimal refund = buyOrderValue.subtract(executionValue);
                // NOTE: Can just deduct execution price * qty from total
                //      balance above and then add refund to available
                //      balance here but it neither reduces the complexity
                //      of the logic nor the number of lines. So, doing it
                //      the way which makes the operations clearer.
                wallet.addToAvailableBalance(refund);
                wallet.addToTotalBalance(refund);
            }
        }
        // If for some reason, the status remains Status.NEW, log it here.
        else {
            logger.info("Unexpected state while settling a BUY.");
        }
    }

    public void settleSell(Order sellOrder,
                           BigDecimal executionPrice,
                           int qtyFulfilled) {
        UserWallet wallet = userRepository.findUserByID(sellOrder.getUserId())
                .get();

        if (sellOrder.getStatus() == Status.PARTIALLY_FILLED ||
                sellOrder.getStatus() == Status.FILLED) {
            wallet.tryDeductFromTotalShares(qtyFulfilled);
            wallet.addToAvailableBalance(sellOrder.getPrice().multiply(
                    BigDecimal.valueOf(qtyFulfilled)));
            wallet.addToTotalBalance(sellOrder.getPrice().multiply(
                    BigDecimal.valueOf(qtyFulfilled)));

            // Refund for aggressive cross-edge
            if(sellOrder.getPrice().compareTo(executionPrice) < 0) {
                BigDecimal sellOrderValue = sellOrder.getPrice().multiply(
                        BigDecimal.valueOf(qtyFulfilled));
                BigDecimal executionValue = executionPrice.multiply(
                        BigDecimal.valueOf(qtyFulfilled));
                BigDecimal refund = executionValue.subtract(sellOrderValue);
                wallet.addToTotalBalance(refund);
                wallet.addToAvailableBalance(refund);
            }
        }
        // If for some reason, the status remains Status.NEW, log it here.
        else {
            logger.info("Unexpected state while settling a SELL.");
        }
    }

    public void settleCancellation(Order cancelledOrder) {
        settlementHandler.execute(() -> {
            UserWallet wallet = userRepository.findUserByID(
                    cancelledOrder.getUserId()).get();
            if (cancelledOrder.getType() == Type.BUY) {
                wallet.addToAvailableBalance(cancelledOrder.getPrice().multiply(
                        BigDecimal.valueOf(cancelledOrder.getQuantity())));
            }
            else {
                wallet.addToAvailableShares(cancelledOrder.getQuantity());
            }
        });
    }
}
