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
    public void settle(Order restingOrder, Order incomingOrder) {
        settlementHandler.execute(() -> {
            if(restingOrder.getType() == Type.BUY) {
                settleBuy(restingOrder, restingOrder.getPrice());
                settleSell(incomingOrder, restingOrder.getPrice());
            }
            else {
                settleBuy(incomingOrder, restingOrder.getPrice());
                settleSell(restingOrder, restingOrder.getPrice());
            }
        });
    }

    public void settleBuy(Order buyOrder, BigDecimal executionPrice) {
        UserWallet wallet = userRepository.findUserByID(buyOrder.getUserId())
                .get();
        int qtyFilled = buyOrder.getQuantity() - buyOrder.getRemainingQuantity();

        if (buyOrder.getStatus() == Status.PARTIALLY_FILLED ||
                buyOrder.getStatus() == Status.FILLED) {
            wallet.addToAvailableShares(qtyFilled);
            wallet.addToTotalShares(qtyFilled);
            // Ignore the boolean return value since we know for sure that
            // the funds are present
            wallet.tryDeductFromTotalBalance(executionPrice.multiply(
                    BigDecimal.valueOf(qtyFilled)));
        }
        // If for some reason, the status remains Status.NEW, log it here.
        else {
            logger.info("Unexpected state while settling a BUY.");
        }
    }

    public void settleSell(Order sellOrder, BigDecimal executionPrice) {
        UserWallet wallet = userRepository.findUserByID(sellOrder.getUserId())
                .get();
        int qtyFilled = sellOrder.getQuantity() - sellOrder.getRemainingQuantity();

        if (sellOrder.getStatus() == Status.PARTIALLY_FILLED ||
                sellOrder.getStatus() == Status.FILLED) {
            wallet.tryDeductFromTotalShares(qtyFilled);
            wallet.addToAvailableBalance(executionPrice.multiply(
                    BigDecimal.valueOf(qtyFilled)));
            wallet.addToTotalBalance(executionPrice.multiply(
                    BigDecimal.valueOf(qtyFilled)));
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
