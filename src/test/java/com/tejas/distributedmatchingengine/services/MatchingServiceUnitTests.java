package com.tejas.distributedmatchingengine.services;

import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.Type;
import com.tejas.distributedmatchingengine.repositories.OrderRepository;
import com.tejas.distributedmatchingengine.repositories.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MatchingServiceUnitTests {
    @Mock
    OrderRepository orderRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    SettlementService settlementService;

    @InjectMocks
    MatchingService matchingService;

    // Matching BUY order
    @Test
    @DisplayName("Test that a BUY order is successfully matched with a SELL" +
            " order.")
    public void matchBuyHappyFlow() {
        Order buyOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        Order sellOrder = new Order("user124",
                Type.SELL, BigDecimal.valueOf(12.00), 20);

        Queue<Order> sellOrderQueue = new ArrayDeque<Order>();
        sellOrderQueue.add(sellOrder);

        SortedMap<BigDecimal, Queue<Order>> suitableOrdersMap =
                new TreeMap<>(Comparator.naturalOrder());
        suitableOrdersMap.put(BigDecimal.valueOf(12.00), sellOrderQueue);

        given(orderRepository.getSellOrders(buyOrder.getPrice()))
                .willReturn(suitableOrdersMap);

        Status orderStatus = matchingService.matchBuy(buyOrder);

        assertEquals(Status.FILLED, orderStatus);
    }

    @Test
    @DisplayName("Test that a BUY order is successfully and fully filled by" +
            " multiple SELL orders.")
    public void matchBuyMultipleSellHappyFlow() {
        Order buyOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        Order sellOrder1 = new Order("user124",
                Type.SELL, BigDecimal.valueOf(12.00), 5);
        Order sellOrder2 = new Order("user125",
                Type.SELL, BigDecimal.valueOf(12.00), 5);
        Order sellOrder3 = new Order("user126",
                Type.SELL, BigDecimal.valueOf(12.00), 5);
        Order sellOrder4 = new Order("user124",
                Type.SELL, BigDecimal.valueOf(12.00), 5);

        Queue<Order> sellOrderQueue = new ArrayDeque<Order>(List.of(sellOrder1,
                sellOrder2, sellOrder3, sellOrder4));

        SortedMap<BigDecimal, Queue<Order>> suitableOrdersMap =
                new TreeMap<>(Comparator.naturalOrder());
        suitableOrdersMap.put(BigDecimal.valueOf(12.00), sellOrderQueue);

        given(orderRepository.getSellOrders(buyOrder.getPrice()))
                .willReturn(suitableOrdersMap);

        Status orderStatus = matchingService.matchBuy(buyOrder);

        assertEquals(Status.FILLED, orderStatus);
    }

    @Test
    @DisplayName("Test that a BUY order is successfully matched and partially" +
            " filled with a SELL order.")
    public void matchBuyPartialFillHappyFlow() {
        Order buyOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        Order sellOrder = new Order("user124",
                Type.SELL, BigDecimal.valueOf(12.00), 10);

        Queue<Order> sellOrderQueue = new ArrayDeque<Order>();
        sellOrderQueue.add(sellOrder);

        SortedMap<BigDecimal, Queue<Order>> suitableOrdersMap =
                new TreeMap<>(Comparator.naturalOrder());
        suitableOrdersMap.put(BigDecimal.valueOf(12.00), sellOrderQueue);

        given(orderRepository.getSellOrders(buyOrder.getPrice()))
                .willReturn(suitableOrdersMap);

        Status orderStatus = matchingService.matchBuy(buyOrder);

        assertEquals(Status.PARTIALLY_FILLED, orderStatus);
    }

    @Test
    @DisplayName("Test that a BUY order is successfully and partially filled" +
            " by multiple SELL orders.")
    public void matchBuyMultipleSellPartialHappyFlow() {
        Order buyOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        Order sellOrder1 = new Order("user124",
                Type.SELL, BigDecimal.valueOf(12.00), 5);
        Order sellOrder2 = new Order("user125",
                Type.SELL, BigDecimal.valueOf(12.00), 5);
        Order sellOrder3 = new Order("user126",
                Type.SELL, BigDecimal.valueOf(12.00), 5);

        Queue<Order> sellOrderQueue = new ArrayDeque<Order>(List.of(sellOrder1,
                sellOrder2, sellOrder3));

        SortedMap<BigDecimal, Queue<Order>> suitableOrdersMap =
                new TreeMap<>(Comparator.naturalOrder());
        suitableOrdersMap.put(BigDecimal.valueOf(12.00), sellOrderQueue);

        given(orderRepository.getSellOrders(buyOrder.getPrice()))
                .willReturn(suitableOrdersMap);

        Status orderStatus = matchingService.matchBuy(buyOrder);

        assertEquals(Status.PARTIALLY_FILLED, orderStatus);
    }

    @Test
    @DisplayName("Test that a BUY order is added to order book when the SELL" +
            " order queue is empty.")
    public void matchBuyEmptySell() {
        Order buyOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        // Empty SELL Queue
        Queue<Order> sellOrderQueue = new ArrayDeque<Order>();

        SortedMap<BigDecimal, Queue<Order>> suitableOrdersMap =
                new TreeMap<>(Comparator.naturalOrder());
        suitableOrdersMap.put(BigDecimal.valueOf(12.00), sellOrderQueue);

        given(orderRepository.getSellOrders(buyOrder.getPrice()))
                .willReturn(suitableOrdersMap);

        Status orderStatus = matchingService.matchBuy(buyOrder);

        assertEquals(Status.NEW, orderStatus);

        verify(orderRepository).addBuyOrder(buyOrder);
    }

    @Test
    @DisplayName("Test that a BUY order is added to order book when the SELL" +
            " order queue is empty.")
    public void matchBuyNoSell() {
        Order buyOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        // Empty asks book
        SortedMap<BigDecimal, Queue<Order>> suitableOrdersMap =
                new TreeMap<>(Comparator.naturalOrder());

        given(orderRepository.getSellOrders(buyOrder.getPrice()))
                .willReturn(suitableOrdersMap);

        Status orderStatus = matchingService.matchBuy(buyOrder);

        assertEquals(Status.NEW, orderStatus);

        verify(orderRepository).addBuyOrder(buyOrder);
    }


}
