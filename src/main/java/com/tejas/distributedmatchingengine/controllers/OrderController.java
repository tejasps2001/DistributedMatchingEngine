package com.tejas.distributedmatchingengine.controllers;

import com.tejas.distributedmatchingengine.DTOs.OrderMatchResponse;
import com.tejas.distributedmatchingengine.DTOs.PlaceOrderRequest;
import com.tejas.distributedmatchingengine.exceptions.UserNotFoundException;
import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.Type;
import com.tejas.distributedmatchingengine.models.UserWallet;
import com.tejas.distributedmatchingengine.repositories.UserRepository;
import com.tejas.distributedmatchingengine.services.MatchingService;
import jakarta.validation.Valid;
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
    
    private static final String ORDER_ID = "orderID";

    public OrderController(MatchingService matchingService,
                           UserRepository userRepository) {
        this.matchingService = matchingService;
        this.userRepository = userRepository;
    }

    @PostMapping("/sell")
    public ResponseEntity<String> placeSell(@Valid @RequestBody PlaceOrderRequest req) {
        logger.info("Received SELL order.");
        UserWallet wallet = userRepository.findUserByID(req.userid())
                .orElseThrow(() -> new UserNotFoundException("No user with" +
                        " userid:" + req.userid()));
        if (washTradingAttempt(wallet, Type.BUY)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Order cancelled due to self-trade prevention.");
        }
        Order newSellOrder = new Order(req.userid(),
                Type.SELL, BigDecimal.valueOf(req.price()), req.quantity());
        // Check if the user contains the required amount of shares
        if (wallet.tryDeductFromAvailableShares(newSellOrder.getQuantity())) {
            wallet.addOrderToWallet(newSellOrder);
            OrderMatchResponse matchResult = matchingService.matchSell(newSellOrder);
            return craftResponse(matchResult, newSellOrder);
        } else {
            newSellOrder.setStatus(Status.CANCELLED);
            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_CONTENT)
                    .body("Insufficient amount of shares!");
        }
    }

    @PostMapping("/buy")
    public ResponseEntity<String> placeBuy(@Valid @RequestBody PlaceOrderRequest req) {
        logger.info("Received BUY order.");
        UserWallet wallet = userRepository.findUserByID(req.userid())
                .orElseThrow(() -> new UserNotFoundException("No user with" +
                        " userid:" + req.userid()));
        Order newBuyOrder = new Order(req.userid(),
                Type.BUY, BigDecimal.valueOf(req.price()), req.quantity());
        // Check if the user has the money to buy
        if (wallet.tryDeductFromAvailableBalance(newBuyOrder.getPrice().multiply(
                BigDecimal.valueOf(newBuyOrder.getQuantity())
        ))) {
            wallet.addOrderToWallet(newBuyOrder);
            OrderMatchResponse matchResult = matchingService.matchBuy(newBuyOrder);
            return craftResponse(matchResult, newBuyOrder);
        } else {
            newBuyOrder.setStatus(Status.CANCELLED);
            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_CONTENT)
                    .body("Insufficient funds!");
        }
    }

    @DeleteMapping("/cancel")
    public ResponseEntity<String> cancelOrder(@RequestParam long orderID) {
        logger.info("Received request for cancellation.");
        var cancelledOrderStatus = matchingService.cancelOrder(orderID);
        if (cancelledOrderStatus == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(String.format("Order with %d not found.", orderID));
        }
        return switch (cancelledOrderStatus) {
            case Status.CANCELLED -> ResponseEntity
                    .status(HttpStatus.OK)
                    .body("Order cancelled.");
            case Status.FILLED, Status.PARTIALLY_FILLED -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Couldn't cancel. Order already filled.");
            default -> ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected order status.");
        };
    }

    private ResponseEntity<String> craftResponse(OrderMatchResponse matchResponse,
                                                 Order incomingOrder) {
        var responseStatus = matchResponse.status();
        String partialExecution = incomingOrder.getType() == Type.BUY ?
                "Bought %d shares." : "Filled %d shares.";
        return switch (responseStatus) {
            case Status.NEW -> ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .header(ORDER_ID,
                            String.valueOf(incomingOrder.getOrderId()))
                    .body("Couldn't fill order. Added to order book.");
            case Status.PARTIALLY_FILLED -> ResponseEntity
                    .status(HttpStatus.OK)
                    .header(ORDER_ID,
                            String.valueOf(incomingOrder.getOrderId()))
                    .body(String.format(partialExecution,
                            matchResponse.filledQuantity()));
            case Status.FILLED -> ResponseEntity
                    .status(HttpStatus.OK)
                    .header(ORDER_ID,
                            String.valueOf(incomingOrder.getOrderId()))
                    .body("Order executed.");
            default -> ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("There was a problem with the server.");
        };
    }
}
