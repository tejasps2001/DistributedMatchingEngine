package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderRepositoryUnitTests {
    private SortedMap<BigDecimal, Queue<Order>> bidsBook;
    private SortedMap<BigDecimal, Queue<Order>> asksBook;
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        bidsBook = new TreeMap<>(Comparator.reverseOrder());
        asksBook = new TreeMap<>(Comparator.naturalOrder());
        orderRepository = new OrderRepository(bidsBook, asksBook);
    }

    @Test
    @DisplayName("Test that a new BUY order is added to the bids book.")
    void addBuyToBids() {
        OrderRepository spiedOrderRepository = spy(new OrderRepository());

        doNothing().when(spiedOrderRepository).addOrder(any(), any());

        Order buyOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(11.00), 10);

        spiedOrderRepository.addBuyOrder(buyOrder);

        verify(spiedOrderRepository).addOrder(buyOrder, bidsBook);
    }

    @Test
    @DisplayName("Test that a new SELL order is added to the asks book.")
    void addSellToAsks() {
        OrderRepository spiedOrderRepository = spy(new OrderRepository());

        doNothing().when(spiedOrderRepository).addOrder(any(), any());

        Order sellOrder = new Order("user123",
                Type.SELL, BigDecimal.valueOf(11.00), 10);

        spiedOrderRepository.addSellOrder(sellOrder);

        verify(spiedOrderRepository).addOrder(sellOrder, asksBook);
    }

    @Test
    @DisplayName("Test whether an order is added to a new order queue in the" +
            " order book.")
    void addToNewOrderQueue() {
        Order buyOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(11.00), 10);

        assertTrue(bidsBook.isEmpty());

        orderRepository.addOrder(buyOrder, bidsBook);

        assertTrue(bidsBook.containsKey(buyOrder.getPrice()));
        assertEquals(bidsBook.get(buyOrder.getPrice()).poll(), buyOrder);
    }

    @Test
    @DisplayName("Test whether an order is added to an existing order queue in" +
            " the order book.")
    void addToExistingOrderQueue() {
        Order buyOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(11.00), 10);

        Queue<Order> buyOrderQueue = new ArrayDeque<>();
        buyOrderQueue.add(buyOrder);
        bidsBook.put(BigDecimal.valueOf(11.00), buyOrderQueue);

        assertFalse(bidsBook.isEmpty());

        orderRepository.addOrder(buyOrder, bidsBook);

        assertTrue(bidsBook.containsKey(buyOrder.getPrice()));
        assertEquals(bidsBook.get(buyOrder.getPrice()).poll(), buyOrder);
    }
}
