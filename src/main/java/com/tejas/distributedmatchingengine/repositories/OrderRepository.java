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
        bidsBook = new TreeMap<>(Comparator.reverseOrder());
        asksBook = new TreeMap<>(Comparator.naturalOrder());
    }

    // Package-private constructor for testing purposes
    OrderRepository(SortedMap<BigDecimal, Queue<Order>> bidsBook,
                    SortedMap<BigDecimal, Queue<Order>> asksBook) {
        this.bidsBook = bidsBook;
        this.asksBook = asksBook;
    }

    /**
     * Add a BUY order to the order book. This should be called if the order
     * couldn't be immediately matched with any existing resting SELL order.
     *
     * @param newBuyOrder A new, unmatched resting BUY order
     */
    public void addBuyOrder(Order newBuyOrder) {
        addOrder(newBuyOrder, bidsBook);
    }

    /**
     * Add a SELL order to the order book. This should be called if the order
     * couldn't be immediately matched with any existing resting BUY order.
     *
     * @param newSellOrder A new, unmatched resting SELL order
     */
    public void addSellOrder(Order newSellOrder) {
        addOrder(newSellOrder, asksBook);
    }

    public void addOrder(Order newOrder,
                         SortedMap<BigDecimal, Queue<Order>> orderBook) {
        if (orderBook.get(newOrder.getPrice()) != null) {
            var orderList = orderBook.get(newOrder.getPrice());
            orderList.add(newOrder);
            return;
        }
        var newQueue = new ArrayDeque<Order>();
        newQueue.add(newOrder);
        orderBook.put(newOrder.getPrice(), newQueue);
    }

    public Optional<Boolean> cancelOrder(long orderID) {
        for (Queue<Order> orderQueue : bidsBook.values()) {
            for (Order order : orderQueue) {
                if(order.getOrderId() == orderID) {
                    if (order.getStatus() == Status.NEW) {
                        order.setStatus(Status.CANCELLED);
                        return Optional.of(true);
                    } else if (order.getStatus() == Status.PARTIALLY_FILLED) {
                        return Optional.of(false);
                    }
                }
            }
        }
        for (Queue<Order> orderQueue : asksBook.values()) {
            for (Order order : orderQueue) {
                if(order.getOrderId() == orderID) {
                    if (order.getStatus() == Status.NEW) {
                        order.setStatus(Status.CANCELLED);
                        return Optional.of(true);
                    } else if (order.getStatus() == Status.PARTIALLY_FILLED) {
                        return Optional.of(false);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
