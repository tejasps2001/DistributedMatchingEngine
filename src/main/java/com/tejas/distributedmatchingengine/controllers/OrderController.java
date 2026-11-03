package com.tejas.distributedmatchingengine.controllers;

import com.tejas.distributedmatchingengine.exceptions.UserNotFoundException;
import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.UserWallet;
import com.tejas.distributedmatchingengine.repositories.UserRepository;
import com.tejas.distributedmatchingengine.services.MatchingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.logging.Logger;

@RestController
public class OrderController {
    private final MatchingService matchingService;
    private final UserRepository userRepository;

    private static final Logger logger = Logger.getLogger(
            OrderController.class.getName());

    public OrderController(MatchingService matchingService,
                           UserRepository userRepository) {
        this.matchingService = matchingService;
        this.userRepository = userRepository;
    }

    @PostMapping("/sell")
    public ResponseEntity<String> placeSell(@RequestBody Order newSellOrder) {
        logger.info("Received SELL order.");
        UserWallet wallet = userRepository.findUserByID(newSellOrder.getUserId())
                .orElseThrow(() -> new UserNotFoundException("No user with" +
                        " userID:" + newSellOrder.getUserId()));
        // Check if the user contains the required amount of shares
        if (wallet.tryDeductFromAvailableShares(newSellOrder.getQuantity())) {
            wallet.addOrderToWallet(newSellOrder);
            Status matchStatus = matchingService.matchSell(newSellOrder);
            return switch (matchStatus) {
                case Status.NEW -> ResponseEntity
                        .status(HttpStatus.ACCEPTED)
                        .body("Couldn't fill order. Added to order book.");
                case Status.PARTIALLY_FILLED -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(String.format("Filled %d shares.",
                                newSellOrder.getQuantity() -
                                        newSellOrder.getRemainingQuantity()));
                case Status.FILLED -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body("Order executed.");
                default -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No/Illegal Status provided.");
            };
        } else {
            newSellOrder.cancel();
            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_CONTENT)
                    .body("Insufficient amount of shares!");
        }
    }

    @PostMapping("/buy")
    public ResponseEntity<String> placeBuy(@RequestBody Order newBuyOrder) {
        logger.info("Received BUY order.");
        UserWallet wallet = userRepository.findUserByID(newBuyOrder.getUserId())
                .orElseThrow(() -> new UserNotFoundException("No user with" +
                        " userID:" + newBuyOrder.getUserId()));
        // Check if the user has the money to buy
        if (wallet.tryDeductFromAvailableBalance(newBuyOrder.getPrice().multiply(
                BigDecimal.valueOf(newBuyOrder.getQuantity())
        ))) {
            wallet.addOrderToWallet(newBuyOrder);
            Status matchStatus = matchingService.matchBuy(newBuyOrder);
            return switch (matchStatus) {
                case Status.NEW -> ResponseEntity
                        .status(HttpStatus.ACCEPTED)
                        .body("Couldn't fill order. Added to order book.");
                case Status.PARTIALLY_FILLED -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(String.format("Bought %d shares.",
                                newBuyOrder.getQuantity() - newBuyOrder
                                        .getRemainingQuantity()));
                case Status.FILLED -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body("Order executed.");
                default -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No/Illegal Status provided.");
            };
        } else {
            newBuyOrder.cancel();
            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_CONTENT)
                    .body("Insufficient funds!");
        }
    }

    @DeleteMapping("/cancel")
    public ResponseEntity<String> cancelOrder(@RequestBody Order toCancelOrder) {
        logger.info("Received request for cancellation.");
        Status cancelStatus = toCancelOrder.cancel();
        return switch (cancelStatus) {
            case Status.CANCELLED -> ResponseEntity
                    .status(HttpStatus.OK)
                    .body("Order cancelled.");
            case Status.FILLED -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Couldn't cancel. Order already filled.");
            default -> throw new IllegalStateException("Unexpected value for" +
                    " cancelStatus: " + cancelStatus);
        };
    }
}
