package com.tejas.distributedmatchingengine.services;

import com.tejas.distributedmatchingengine.DTOs.OrderMatchResponse;
import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.Transaction;
import com.tejas.distributedmatchingengine.repositories.TransactionRepository;
import lombok.Getter;
import org.springframework.stereotype.Service;
import com.tejas.distributedmatchingengine.repositories.OrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class MatchingService {
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final SettlementService settlementService;
    private static final Logger logger =
            Logger.getLogger(MatchingService.class.getName());

    // Mark the sequence of transactions occurring by an ID in the matching
    // thread itself instead of using a static variable.
    // Reason: Matching thread A may execute an order later than matching
    // thread B. But matching thread A may acquire the lock on the
    // transactionRecorder executor first.
    // Noting the transactionID in the matching thread itself allows for
    // the proper sequence of transactions.
    @Getter
    private static final AtomicLong nextTransactionID = new AtomicLong(1);

    public MatchingService(OrderRepository orderRepository,
                           SettlementService settlementService,
                           TransactionRepository transactionRepository) {
        this.orderRepository = orderRepository;
        this.settlementService = settlementService;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Match a new BUY order with a SELL order.
     * Orders are passed in concurrently by orderController. But they must be
     * matched synchronously by a single thread. There is no concurrent
     * matching because it would cause inconsistent order books (LMAX).
     *
     * @param newOrder A new, unmatched BUY order
     * @return Status of the order after a successful
     * (or unsuccessful) match
     */
    public synchronized OrderMatchResponse matchBuy(Order newOrder) {
        var asksBook = orderRepository.getAsksBook();
        // No orders to match to; add to order book
        if (asksBook == null) {
            logger.info("No sell orders to match to!");
            orderRepository.addBuyOrder(newOrder);
            return new OrderMatchResponse(newOrder.getStatus(),
                    Collections.emptyList());
        }
        List<Transaction> matches = matchOrders(newOrder, asksBook);

        // Order is not filled, add to book as a resting order
        if (newOrder.getStatus() == Status.PARTIALLY_FILLED ||
                newOrder.getStatus() == Status.NEW) {
            orderRepository.addBuyOrder(newOrder);
        }
        return new OrderMatchResponse(newOrder.getStatus(), matches);
    }

    /**
     * Match a new SELL order with a BUY order.
     * Orders are passed in concurrently by orderController. But they must be
     * matched synchronously by a single thread. There is no concurrent
     * matching because it would cause inconsistent order books (LMAX).
     *
     * @param newOrder A new, unmatched SELL order
     * @return Status of the order after a successful
     * (or unsuccessful) match
     */
    public synchronized OrderMatchResponse matchSell(Order newOrder) {
        var bidsBook = orderRepository.getBidsBook();
        // No orders to match to; add to order book
        if (bidsBook == null) {
            logger.info("No buy orders to match to!");
            orderRepository.addSellOrder(newOrder);
            return new OrderMatchResponse(newOrder.getStatus(),
                    Collections.emptyList());
        }
        List<Transaction> matches = matchOrders(newOrder, bidsBook);

        // If the order is not filled, add to book as a resting order
        if (newOrder.getStatus() == Status.PARTIALLY_FILLED ||
                newOrder.getStatus() == Status.NEW) {
            orderRepository.addSellOrder(newOrder);
        }
        return new OrderMatchResponse(newOrder.getStatus(), matches);
    }

    // Route cancellation through the matching thread since the thread-unsafe
    // order books need to be accessed. Only the matching thread accesses and
    // modifies the order books.
    public synchronized Status cancelOrder(long orderID) {
        return orderRepository.cancelOrder(orderID);
    }

    public List<Transaction> matchOrders(Order newOrder,
                                         SortedMap<BigDecimal,
                                                 Queue<Order>> orderBook) {
        List<Transaction> matches = new ArrayList<>();
        Iterator<Map.Entry<BigDecimal, Queue<Order>>> priceLevels =
                orderBook.entrySet().iterator();
        while (priceLevels.hasNext()) {
            var priceLevel = priceLevels.next();
            // If the price level in the order book is beyond the incoming order's
            // price then we stop matching & return since no more matches is
            // possible
            if (hasNoMatchableOrders(newOrder, priceLevel.getKey()))
                return matches;
            var orderQueue = priceLevel.getValue();
            // Remove the orderQueue if it is empty
            if (orderQueue.isEmpty()) {
                // Remove the empty order queue from the book
                priceLevels.remove();
                continue;
            }

            while (!orderQueue.isEmpty()) {
                Order order = orderQueue.peek();
                if (order.getStatus() == Status.CANCELLED) {
                    settlementService.settleCancellation(order);
                    orderQueue.poll();
                    continue;
                }
                // Self-Trade Prevention
                // Cancel the incoming order
                if (Objects.equals(order.getUserId(), newOrder.getUserId())) {
                    newOrder.setStatus(Status.CANCELLED);
                    settlementService.settleCancellation(newOrder);
                    return Collections.emptyList();
                }
                String msg = String.format("\n%s is suitable for the" +
                                " incoming %s.", order.toCompactString(),
                        newOrder.toCompactString());
                logger.log(Level.INFO, msg);
                var qtyFulfilled = Math.min(newOrder.getRemainingQuantity(),
                        order.getRemainingQuantity());
                var qtyLeft = newOrder.getRemainingQuantity() -
                        order.getRemainingQuantity();
                if (qtyLeft == 0) {
                    newOrder.setRemainingQuantity(0);
                    newOrder.setStatus(Status.FILLED);
                    order.setRemainingQuantity(0);
                    order.setStatus(Status.FILLED);
                    Transaction newTransaction = recordAndSettle(order,
                            newOrder, qtyFulfilled);
                    matches.add(newTransaction);
                    orderQueue.poll();
                    return matches;
                } else if (qtyLeft > 0) {
                    newOrder.setRemainingQuantity(qtyLeft);
                    newOrder.setStatus(Status.PARTIALLY_FILLED);
                    order.setRemainingQuantity(0);
                    order.setStatus(Status.FILLED);
                    Transaction newTransaction = recordAndSettle(order,
                            newOrder, qtyFulfilled);
                    matches.add(newTransaction);
                    orderQueue.poll();
                } else {
                    newOrder.setRemainingQuantity(0);
                    newOrder.setStatus(Status.FILLED);
                    order.setRemainingQuantity(-1 * qtyLeft);
                    order.setStatus(Status.PARTIALLY_FILLED);
                    Transaction newTransaction = recordAndSettle(order,
                            newOrder, qtyFulfilled);
                    matches.add(newTransaction);
                    return matches;
                }
            }
        }
        return Collections.emptyList();
    }

    private Transaction recordAndSettle(Order restingOrder,
                                        Order incomingOrder,
                                        int qtyFulfilled) {
        long transactionID = nextTransactionID.getAndIncrement();
        Instant executionTime = Instant.now();
        settlementService.settle(restingOrder, incomingOrder, qtyFulfilled);
        Transaction newTransaction = new Transaction(
                transactionID,
                executionTime,
                restingOrder.getOrderId(),
                incomingOrder.getOrderId(),
                restingOrder.getPrice(),
                qtyFulfilled);
        transactionRepository.addTransaction(newTransaction);
        return newTransaction;
    }

    private boolean hasNoMatchableOrders(Order incomingOrder,
                                         BigDecimal price) {
        var priceBound = price.compareTo(incomingOrder.getPrice());
        return switch(incomingOrder.getType()) {
            case BUY -> priceBound > 0;
            case SELL -> priceBound < 0;
        };
    }
}
