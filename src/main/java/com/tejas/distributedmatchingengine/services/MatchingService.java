package com.tejas.distributedmatchingengine.services;

import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.Type;
import com.tejas.distributedmatchingengine.repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import com.tejas.distributedmatchingengine.repositories.OrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

@Service
public class MatchingService {
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final static Queue<Order> buyQueue;
    private final static Queue<Order> sellQueue;
    private final SettlementService settlementService;
    private final static Logger logger =
            Logger.getLogger(MatchingService.class.getName());
    // Mark the sequence of transactions occurring by an ID in the matching
    // thread itself using a static variable.
    // Reason: Matching thread A may execute an order later than matching
    // thread B. But matching thread A may acquire the lock first.
    // Noting the transactionID in the matching thread itself allows for
    // proper sequence of transactions.
    private static final AtomicLong nextTransactionID = new AtomicLong(1);
    private static final ExecutorService transactionRecorder =
            Executors.newSingleThreadExecutor();

    static {
        buyQueue = new ConcurrentLinkedQueue<>();
        sellQueue = new ConcurrentLinkedQueue<>();
    }

    public MatchingService(OrderRepository orderRepository,
                           SettlementService settlementService,
                           TransactionRepository transactionRepository) {
        this.orderRepository = orderRepository;
        this.settlementService = settlementService;
        this.transactionRepository = transactionRepository;
    }

    // ONLY ONE THREAD EXECUTES THIS. The core matching logic should be
    // executed (both buy and sell) only by a single thread. (i.e., no
    // concurrent matching) because the shared order books are updated.
    // Therefore, not synchronizing.
    public synchronized Status matchBuy(Order newOrder) {
        var asksBook = orderRepository.getAsksBook();
        // No orders to match to; add to order book
        if (asksBook == null) {
            logger.info("No sell orders to match to!");
            orderRepository.addBuyOrder(newOrder);
            return newOrder.getStatus();
        }
        matchOrders(newOrder, asksBook);

        // Order is not filled, add to book as a resting order
        if (newOrder.getStatus() == Status.PARTIALLY_FILLED ||
                newOrder.getStatus() == Status.NEW) {
            orderRepository.addBuyOrder(newOrder);
        }
        return newOrder.getStatus();
    }

    public synchronized Status matchSell(Order newOrder) {
        var bidsBook = orderRepository.getBidsBook();
        // No orders to match to; add to order book
        if (bidsBook == null) {
            logger.info("No buy orders to match to!");
            orderRepository.addSellOrder(newOrder);
            return newOrder.getStatus();
        }
        matchOrders(newOrder, bidsBook);

        // Order is not filled, add to book as a resting order
        if (newOrder.getStatus() == Status.PARTIALLY_FILLED ||
                newOrder.getStatus() == Status.NEW) {
            orderRepository.addSellOrder(newOrder);
        }
        return newOrder.getStatus();
    }

    public void matchOrders(Order newOrder,
                            SortedMap<BigDecimal,
                                    Queue<Order>> suitableOrdersMap) {
        for (Map.Entry<BigDecimal, Queue<Order>> suitableOrders :
                suitableOrdersMap.entrySet()) {
            // If the order in the order book goes beyond the order's
            // bound then we stop matching & add the partially filled order to
            // the order book
            if (newOrder.getType() == Type.BUY &&
                    suitableOrders.getKey().compareTo(newOrder.getPrice()) > 0) {
                newOrder.setStatus(Status.PARTIALLY_FILLED);
                return;
            }
            if (newOrder.getType() == Type.SELL &&
                    suitableOrders.getKey().compareTo(newOrder.getPrice()) < 0) {
                newOrder.setStatus(Status.PARTIALLY_FILLED);
                return;
            }
            var orderQueue = suitableOrders.getValue();
            // Return if orderQueue is empty
            if(orderQueue.isEmpty()) {
                logger.info(String.format("No %s to match to!",
                        newOrder.getType() == Type.BUY ? Type.SELL : Type.BUY));
                continue;
            }
            for (Order order : orderQueue) {
                if (order.getStatus() == Status.CANCELLED) {
                    settlementService.settleCancellation(order);
                    orderQueue.poll();
                    continue;
                }
                // Self-Trade Prevention
                // Cancel the incoming order
                if (Objects.equals(order.getUserId(), newOrder.getUserId())) {
                    logger.info("An attempt to self-trade was detected.");
                    logger.info(String.format(
                            "Cancelling %s accordingly.", newOrder));
                    newOrder.cancel();
                    settlementService.settleCancellation(newOrder);
                    continue;
                }
                logger.info(String.format("""
                            \n%s is suitable for the incoming %s.
                        """, order.toCompactString(), newOrder.toCompactString()));
                var qtyFulfilled = Math.min(newOrder.getRemainingQuantity(), order.getRemainingQuantity());
                var qtyLeft = newOrder.getRemainingQuantity() - order.getRemainingQuantity();
                if (qtyLeft == 0) {
                    newOrder.setRemainingQuantity(0);
                    newOrder.setStatus(Status.FILLED);
                    order.setRemainingQuantity(0);
                    order.setStatus(Status.FILLED);
                    // Use effectively final variables to store the serial number
                    // and the execution time and pass it on to the transaction
                    // ledger event loop
                    long transactionID1 = nextTransactionID.getAndIncrement();
                    Instant executionTime1 = Instant.now();
                    settlementService.settle(order, newOrder, qtyFulfilled);
                    transactionRecorder.execute(() ->
                            transactionRepository.addTransaction(
                                    transactionID1,
                                    executionTime1,
                                    order.getOrderId(),
                                    newOrder.getOrderId(),
                                    order.getPrice(),
                                    qtyFulfilled)
                    );
                    orderQueue.poll();
                    return;
                } else if (qtyLeft > 0) {
                    newOrder.setRemainingQuantity(qtyLeft);
                    newOrder.setStatus(Status.PARTIALLY_FILLED);
                    order.setRemainingQuantity(0);
                    order.setStatus(Status.FILLED);
                    // Use effectively final variables to store the serial number
                    // and the execution time and pass it on to the transaction
                    // ledger event loop
                    long transactionID2 = nextTransactionID.getAndIncrement();
                    Instant executionTime2 = Instant.now();
                    settlementService.settle(order, newOrder, qtyFulfilled);
                    transactionRecorder.execute(() ->
                            transactionRepository.addTransaction(
                                    transactionID2,
                                    executionTime2,
                                    order.getOrderId(),
                                    newOrder.getOrderId(),
                                    order.getPrice(),
                                    qtyFulfilled)
                    );
                    orderQueue.poll();
                } else {
                    newOrder.setRemainingQuantity(0);
                    newOrder.setStatus(Status.FILLED);
                    order.setRemainingQuantity(-1 * qtyLeft);
                    order.setStatus(Status.PARTIALLY_FILLED);
                    // Use effectively final variables to store the serial number
                    // and the execution time and pass it on to the transaction
                    // ledger event loop
                    long transactionID3 = nextTransactionID.getAndIncrement();
                    Instant executionTime3 = Instant.now();
                    settlementService.settle(order, newOrder, qtyFulfilled);
                    transactionRecorder.execute(() ->
                            transactionRepository.addTransaction(
                                    transactionID3,
                                    executionTime3,
                                    order.getOrderId(),
                                    newOrder.getOrderId(),
                                    order.getPrice(),
                                    qtyFulfilled)
                    );
                    return;
                }
            }
        }
    }
}
