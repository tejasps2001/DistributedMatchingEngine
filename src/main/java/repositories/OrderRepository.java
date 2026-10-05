package repositories;

import lombok.Getter;
import lombok.Setter;
import models.Order;
import models.Status;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

//@Getter
//@Setter
public class OrderRepository {
    private final SortedMap<BigDecimal, Queue<Order>> bidsBook;
    private final SortedMap<BigDecimal, Queue<Order>> asksBook;
    public OrderRepository() {
        // Bids book sorted in descending order & asks book sorted in ascending order
        bidsBook = new TreeMap<>(Comparator.reverseOrder());
        asksBook = new TreeMap<>(Comparator.naturalOrder());
    }

    /**
     * Get BUY orders that is greater than or equal to the specified price.
     *
     * @param price The minimum price of the resting BUY orders
     * @return a map of orders (sorted by descending price) from the book
     *          that is greater than or equal to {@code price}, or {@code null} if no
     *          resting order satisfies this criterion.
      */
    public SortedMap<BigDecimal, Queue<Order>> getBuyOrders(BigDecimal price) {
        if(bidsBook.get(price) == null) return null;
        // I don't necessarily need to send only "remainingQuantity" number of orders
        // since the receiver can use what it needs and stop, right? So, removing the
        // "remainingQuantity" parameter
        return bidsBook.tailMap(price);
    }

    /**
     * Get SELL orders that is less than or equal to the specific price.
     *
     * @param price The maximum price of the resting SELL orders
     * @return a map of orders (sorted by ascending price) from the book
     *          that greater than or equal to {@code price}, or {@code null} if no
     *          resting order satisfies this criterion.
     */
    public SortedMap<BigDecimal, Queue<Order>> getSellOrders(BigDecimal price) {
        if(asksBook.get(price) == null) return null;
        return asksBook.tailMap(price);
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

    public void addOrder(Order newSellOrder, SortedMap<BigDecimal, Queue<Order>> orderBook) {
        if(orderBook.get(newSellOrder.getPrice()) != null) {
            var orderList = asksBook.get(newSellOrder.getPrice());
            orderList.add(newSellOrder);
            return;
        }
        var newQueue = new ConcurrentLinkedQueue<Order>();
        newQueue.add(newSellOrder);
        orderBook.put(newSellOrder.getPrice(), newQueue);
        // TODO: Don't add it if it isn't cancelled!
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
            // don't need to worry about orders in the book that are cancelled
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
