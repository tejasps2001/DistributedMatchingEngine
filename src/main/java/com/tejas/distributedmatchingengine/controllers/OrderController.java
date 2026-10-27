package com.tejas.distributedmatchingengine.controllers;

import com.tejas.distributedmatchingengine.exceptions.UserNotFoundException;
import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.UserWallet;
import com.tejas.distributedmatchingengine.repositories.UserRepository;
import com.tejas.distributedmatchingengine.services.MatchingService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.logging.Logger;

@RestController
public class OrderController {
    private final MatchingService matchingService;
    private final UserRepository userRepository;

    private final static Logger logger = Logger.getLogger(
            OrderController.class.getName());

    public OrderController(MatchingService matchingService,
                           UserRepository userRepository) {
        this.matchingService = matchingService;
        this.userRepository = userRepository;
    }

    @PostMapping("/sell")
    public String placeSell(@RequestBody Order newSellOrder) {
        logger.info("Received SELL order.");
        UserWallet wallet = userRepository.findUserByID(newSellOrder.getUserId())
                .orElseThrow(() -> new UserNotFoundException("No user with" +
                        " userID:" + newSellOrder.getUserId()));
        // Check if the user contains the required amount of shares
        if (wallet.tryDeductFromAvailableShares(newSellOrder.getQuantity())) {
            // The limit on the number of async tomcat threads will time out
            // requests during high load. Need to add the requests' orders to a
            // queue and release the thread. CompletableFuture is better than a
            // synchronized?
            Status matchStatus = matchingService.matchSell(newSellOrder);
            return switch (matchStatus) {
                case Status.NEW -> "Couldn't fill order. Added to order book.";
                case Status.PARTIALLY_FILLED -> String.format("""
                                Filled %d shares.""",
                        newSellOrder.getQuantity() -
                                newSellOrder.getRemainingQuantity());
                case Status.FILLED -> "Order executed.";
                default -> "No/Illegal Status provided.";
            };
        } else {
            return "Insufficient amount of shares!";
        }
    }

    @PostMapping("/buy")
    public String placeBuy(@RequestBody Order newBuyOrder) {
        logger.info("Received BUY order.");
        // The limit on the number of async tomcat threads will time out requests
        // during high load. Need to add the requests' orders to a queue and
        // release the thread. CompletableFuture is better than a synchronized?
        // TODO: Cancel the order in case of insufficient balance
        //  How to earmark part of the balance when an Order is submitted?
        //  Maybe do the earmarking here? Can't be async since another order
        //  (maybe even for a different symbol even) can be sent from the same
        //  user. But then the crediting and debiting calls are synchronous so
        //  not much needs to be done other than calling them here.
        UserWallet wallet = userRepository.findUserByID(newBuyOrder.getUserId())
                .orElseThrow(() -> new UserNotFoundException("No user with" +
                        " userID:" + newBuyOrder.getUserId()));
        // Check if the user has the money to buy
        if (wallet.tryDeductFromAvailableBalance(newBuyOrder.getPrice().multiply(
                BigDecimal.valueOf(newBuyOrder.getQuantity())
        ))) {
            // The limit on the number of async tomcat threads will time out
            // requests during high load. Need to add the requests' orders to a
            // queue and release the thread. CompletableFuture is better than a
            // synchronized?
            Status matchStatus = matchingService.matchBuy(newBuyOrder);
            return switch (matchStatus) {
                case Status.NEW -> "Couldn't fill order. Added to order book.";
                case Status.PARTIALLY_FILLED -> String.format("""
                                Bought %d shares.""",
                        newBuyOrder.getQuantity() - newBuyOrder
                                .getRemainingQuantity());
                case Status.FILLED -> "Order executed.";
                default -> "No/Illegal Status provided.";
            };
        } else {
            return "Insufficient funds!";
        }
    }

    @DeleteMapping("/cancel")
    public String cancelOrder(@RequestBody Order toCancelOrder) {
        logger.info("Received request for cancellation.");
        // If order already filled, then inform user with success code
        Status cancelStatus = toCancelOrder.cancel();
        return switch (cancelStatus) {
            case Status.CANCELLED -> "Order cancelled.";
            case Status.FILLED -> "Couldn't cancel. Order already filled.";
            default -> throw new IllegalStateException("Unexpected value for" +
                    " cancelStatus: " + cancelStatus);
        };
    }

    @GetMapping("/portfolio")
    // Get the total and the available balance of the user
    public String getPortfolio(@RequestParam String userID) {
        UserWallet wallet = userRepository.findUserByID(userID)
                .orElseThrow(() -> new UserNotFoundException("No user with" +
                        " userID:" + userID));
        return wallet.toString();
    }

    @PostMapping
    // Create a new user and return the userID of the created account
    public String createNewUser(@RequestParam double balance) {
        if (balance < 0) return "Negative balance not allowed!";
        return userRepository.addUser(BigDecimal.valueOf(balance));
    }
}
