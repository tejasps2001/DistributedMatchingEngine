package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@Repository
public class OrderRepository {
    private final SortedMap<BigDecimal, Queue<Order>> bidsBook;
    private final SortedMap<BigDecimal, Queue<Order>> asksBook;

    public OrderRepository() {
        // Bids book sorted in descending order & asks book sorted in ascending order
        bidsBook = new TreeMap<>(Comparator.reverseOrder());
        asksBook = new TreeMap<>(Comparator.naturalOrder());
    }

    /**
     * Add a resting BUY order to the order book. This should be called if the order
     * couldn't be immediately matched with any existing resting SELL order.
     *
     * @param newBuyOrder A new, unmatched resting BUY order
     */
    public void addBuyOrder(Order newBuyOrder) {
        addOrder(newBuyOrder, bidsBook);
    }

    /**
     * Add a resting SELL order to the order book. This should be called if the order
     * couldn't be immediately matched with any existing resting BUY order.
     *
     * @param newSellOrder A new, unmatched resting SELL order
     */
    public void addSellOrder(Order newSellOrder) {
        addOrder(newSellOrder, asksBook);
    }

    public void addOrder(Order newOrder, SortedMap<BigDecimal, Queue<Order>> orderBook) {
        if(orderBook.get(newOrder.getPrice()) != null) {
            var orderList = orderBook.get(newOrder.getPrice());
            orderList.add(newOrder);
            return;
        }
        var newQueue = new ArrayDeque<Order>();
        newQueue.add(newOrder);
        orderBook.put(newOrder.getPrice(), newQueue);
        // TODO: Don't add it if it is cancelled!
    }

    // A list is enough. No need of a queue.
    public void removeBuyOrders(SortedMap<BigDecimal, Queue<Order>> buyOrders) {
        removeOrders(buyOrders, bidsBook);
    }

    public void removeSellOrders(SortedMap<BigDecimal, Queue<Order>> sellOrders) {
        removeOrders(sellOrders, asksBook);
    }

    public void removeOrders(SortedMap<BigDecimal, Queue<Order>> ordersToBeRemoved,
                             SortedMap<BigDecimal, Queue<Order>> orderBook) {
        for(Map.Entry<BigDecimal, Queue<Order>> orderQueueToBeRemoved :
                ordersToBeRemoved.entrySet()) {
            var orderQueue = orderBook.get(orderQueueToBeRemoved.getKey());
            // Don't remove orders from the order book that are only partially
            // filled.
            // don't need to worry about orders in the book that are canceled
            // after matching because ordersToBeRemoved have the exact orders
            // that were matched
            var matchedQueue = orderQueueToBeRemoved.getValue();
            // TODO: removeIf() bulk operation is not guaranteed to be
            //  performed atomically
            matchedQueue.removeIf(order ->
                    order.getStatus() == Status.PARTIALLY_FILLED);
            orderBook.get(orderQueueToBeRemoved.getKey()).
                    removeAll(matchedQueue);
        }
    }
}
