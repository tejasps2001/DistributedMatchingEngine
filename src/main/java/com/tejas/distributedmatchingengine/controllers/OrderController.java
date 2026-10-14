package com.tejas.distributedmatchingengine.controllers;

import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.services.MatchingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

@RestController
public class OrderController {
    private final MatchingService matchingService;

    private final static Logger logger = Logger.getLogger(OrderController.class.getName());

    public OrderController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping("/sell")
    public String placeSell(@RequestBody Order newSellOrder) {
        logger.info("Received SELL order.");
        // The limit on the number of async tomcat threads will time out requests
        // during high load. Need to add the requests' orders to a queue and
        // release the thread. CompletableFuture is better than a synchronized?
        Status matchStatus = matchingService.matchSell(newSellOrder);
        return switch (matchStatus) {
            case Status.NEW -> "Couldn't fill order. Added to order book.";
            case Status.PARTIALLY_FILLED -> String.format("""
                    Filled %d shares.""",
                    newSellOrder.getQuantity() - newSellOrder.getRemainingQuantity());
            case Status.FILLED -> "Order executed.";
            default -> "No/Illegal Status provided.";
        };
    }

    @PostMapping("/buy")
    public String placeBuy(@RequestBody Order newBuyOrder) {
        logger.info("Received BUY order.");
        // The limit on the number of async tomcat threads will time out requests
        // during high load. Need to add the requests' orders to a queue and
        // release the thread. CompletableFuture is better than a synchronized?
        Status matchStatus = matchingService.matchBuy(newBuyOrder);
        return switch (matchStatus) {
            case Status.NEW -> "Couldn't fill order. Added to order book.";
            case Status.PARTIALLY_FILLED -> String.format("""
                    Filled %d shares.""",
                    newBuyOrder.getQuantity() - newBuyOrder.getRemainingQuantity());
            case Status.FILLED -> "Order executed.";
            default -> "No/Illegal Status provided.";
        };
    }
}
