package com.tejas.distributedmatchingengine.services;

import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import org.springframework.stereotype.Service;
import com.tejas.distributedmatchingengine.repositories.OrderRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;

@Service
public class MatchingService {
    private final OrderRepository orderRepository;

    public MatchingService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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
        matchStatus = matchOrders(newOrder, suitableOrdersMap);

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
        if(suitableOrdersMap == null) {
            orderRepository.addSellOrder(newOrder);
            return matchStatus;
        }
        matchStatus = matchOrders(newOrder, suitableOrdersMap);

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
                    orderQueue.poll();
                    return;
                } else if (qtyLeft > 0) {
                    newOrder.setRemainingQuantity(qtyLeft);
                    newOrder.setStatus(Status.PARTIALLY_FILLED);
                    order.setRemainingQuantity(0);
                    order.setStatus(Status.FILLED);
                    orderQueue.poll();
                } else {
                    newOrder.setRemainingQuantity(0);
                    newOrder.setStatus(Status.FILLED);
                    order.setRemainingQuantity(-1 * qtyLeft);
                    order.setStatus(Status.PARTIALLY_FILLED);
                    return 1;
                }
            }
        }
        return 0;
    }
}
