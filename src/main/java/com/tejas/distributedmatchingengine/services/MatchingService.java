package com.tejas.distributedmatchingengine.services;

import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.Transaction;
import com.tejas.distributedmatchingengine.models.Type;
import com.tejas.distributedmatchingengine.repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import com.tejas.distributedmatchingengine.repositories.OrderRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Service
public class MatchingService {
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final static Logger logger =
            Logger.getLogger(MatchingService.class.getName());
    private static final Executor transactionRecorder = Executors.newFixedThreadPool(8);

    public MatchingService(OrderRepository orderRepository) {

    public MatchingService(OrderRepository orderRepository,
                           TransactionRepository transactionRepository) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
    }

    // Return 1 for successful match, 0 for fail or addition to book
    // ONLY ONE THREAD EXECUTES THIS. The core matching logic should be
    // executed (both buy and sell) only by a single thread. (i.e., no
    // concurrent matching) because the shared order books are updated.
    // Therefore, not synchronizing.
    public int matchBuy(Order newOrder) {
        int matchStatus = 0;
        var suitableOrdersMap = orderRepository.getBuyOrders(
                newOrder.getPrice());
        // No orders to match to; add to order book
        if(suitableOrdersMap == null) {
            orderRepository.addSellOrder(newOrder);
            return matchStatus;
        }
//        else {
//            logger.info("Sell orders present because we have: ");
//            for(Map.Entry<BigDecimal, Queue<Order>> suitableOrders: suitableOrdersMap.entrySet()) {
//                logger.info(suitableOrders.getValue().toString());
//            }
//        }
        matchOrders(newOrder, suitableOrdersMap);

        // Order is not filled, add to book as a resting order
        if (newOrder.getStatus() == Status.PARTIALLY_FILLED ||
                newOrder.getStatus() == Status.NEW) {
            orderRepository.addBuyOrder(newOrder);
        }
        return matchStatus;
    }

    // Return 1 for successful match, 0 for fail or addition to book
    public int matchSell(Order newOrder) {
        int matchStatus = 0;
        var suitableOrdersMap = orderRepository.getSellOrders(
                newOrder.getPrice());
        // No orders to match to; add to order book
        if (suitableOrdersMap == null) {
            logger.info("No buy orders to match to!");
            orderRepository.addSellOrder(newOrder);
            return matchStatus;
        }
//        else {
//            logger.info("Buy orders present because we have: ");
//            for(Map.Entry<BigDecimal, Queue<Order>> suitableOrders: suitableOrdersMap.entrySet()) {
//                logger.info(suitableOrders.getValue().toString());
//            }
//        }
        matchOrders(newOrder, suitableOrdersMap);

        // Order is not filled, add to book as a resting order
        if (newOrder.getStatus() == Status.PARTIALLY_FILLED ||
                newOrder.getStatus() == Status.NEW) {
            orderRepository.addSellOrder(newOrder);
        }
        return matchStatus;
    }

    public int matchOrders(Order newOrder,
                            SortedMap<BigDecimal,
                                    Queue<Order>> suitableOrdersMap) {
        for (Map.Entry<BigDecimal, Queue<Order>> suitableOrders :
                suitableOrdersMap.entrySet()) {
            var orderQueue = suitableOrders.getValue();
            for(Order order : orderQueue) {
                if(order.getStatus() == Status.CANCELLED) {
            for (Order order : orderQueue) {
                logger.info(String.format("""
                            \n%s is suitable for the incoming %s.
                        """, order.toCompactString(), newOrder.toCompactString()));
                if (order.getStatus() == Status.CANCELLED) {
                    orderQueue.poll();
                    continue;
                }
                var qtyFulfilled = Math.min(newOrder.getRemainingQuantity(), order.getRemainingQuantity());
                var qtyLeft = newOrder.getRemainingQuantity() - order.getRemainingQuantity();
                if (qtyLeft == 0) {
                    newOrder.setRemainingQuantity(0);
                    newOrder.setStatus(Status.FILLED);
                    order.setRemainingQuantity(0);
                    order.setStatus(Status.FILLED);
                    transactionRecorder.execute(() ->
                            transactionRepository.addTransaction(
                                    new Transaction(order, newOrder, qtyFulfilled)
                            ));
                    orderQueue.poll();
                    return;
                } else if (qtyLeft > 0) {
                    newOrder.setRemainingQuantity(qtyLeft);
                    newOrder.setStatus(Status.PARTIALLY_FILLED);
                    order.setRemainingQuantity(0);
                    order.setStatus(Status.FILLED);
                    transactionRecorder.execute(() ->
                            transactionRepository.addTransaction(
                                    new Transaction(order, newOrder, qtyFulfilled)
                            ));
                    orderQueue.poll();
                } else {
                    newOrder.setRemainingQuantity(0);
                    newOrder.setStatus(Status.FILLED);
                    order.setRemainingQuantity(-1 * qtyLeft);
                    order.setStatus(Status.PARTIALLY_FILLED);
                    transactionRecorder.execute(() ->
                            transactionRepository.addTransaction(
                                    new Transaction(order, newOrder,
                                            -1 * qtyFulfilled)
                            ));
                    return;
                }
            }
        }
        return 0;
    }
}
