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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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

        Queue<Order> sellOrderQueue = new ArrayDeque<>();
        sellOrderQueue.add(sellOrder);

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.naturalOrder());
        orderBook.put(BigDecimal.valueOf(12.00), sellOrderQueue);

        given(orderRepository.getAsksBook())
                .willReturn(orderBook);

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

        Queue<Order> sellOrderQueue = new ArrayDeque<>(List.of(sellOrder1,
                sellOrder2, sellOrder3, sellOrder4));

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.naturalOrder());
        orderBook.put(BigDecimal.valueOf(12.00), sellOrderQueue);

        given(orderRepository.getAsksBook())
                .willReturn(orderBook);

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

        Queue<Order> sellOrderQueue = new ArrayDeque<>();
        sellOrderQueue.add(sellOrder);

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.naturalOrder());
        orderBook.put(BigDecimal.valueOf(12.00), sellOrderQueue);

        given(orderRepository.getAsksBook())
                .willReturn(orderBook);

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

        Queue<Order> sellOrderQueue = new ArrayDeque<>(List.of(sellOrder1,
                sellOrder2, sellOrder3));

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.naturalOrder());
        orderBook.put(BigDecimal.valueOf(12.00), sellOrderQueue);

        given(orderRepository.getAsksBook())
                .willReturn(orderBook);

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
        Queue<Order> sellOrderQueue = new ArrayDeque<>();

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.naturalOrder());
        orderBook.put(BigDecimal.valueOf(12.00), sellOrderQueue);

        given(orderRepository.getAsksBook())
                .willReturn(orderBook);

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
        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.naturalOrder());

        given(orderRepository.getAsksBook())
                .willReturn(orderBook);

        Status orderStatus = matchingService.matchBuy(buyOrder);

        assertEquals(Status.NEW, orderStatus);

        verify(orderRepository).addBuyOrder(buyOrder);
    }

    // Matching SELL order
    @Test
    @DisplayName("Test that a SELL order is successfully matched with a BUY" +
            " order.")
    public void matchSellHappyFlow() {
        Order sellOrder = new Order("user123",
                Type.SELL, BigDecimal.valueOf(12.00), 20);

        Order buyOrder = new Order("user124",
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        Queue<Order> buyOrderQueue = new ArrayDeque<>();
        buyOrderQueue.add(buyOrder);

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.reverseOrder());
        orderBook.put(BigDecimal.valueOf(12.00), buyOrderQueue);

        given(orderRepository.getBidsBook())
                .willReturn(orderBook);

        Status orderStatus = matchingService.matchSell(sellOrder);

        assertEquals(Status.FILLED, orderStatus);
    }

    @Test
    @DisplayName("Test that a SELL order is successfully and fully filled by" +
            " multiple BUY orders.")
    public void matchSellMultipleBuyHappyFlow() {
        Order sellOrder = new Order("user123",
                Type.SELL, BigDecimal.valueOf(12.00), 20);

        Order buyOrder1 = new Order("user124",
                Type.BUY, BigDecimal.valueOf(12.00), 5);
        Order buyOrder2 = new Order("user125",
                Type.BUY, BigDecimal.valueOf(12.00), 5);
        Order buyOrder3 = new Order("user126",
                Type.BUY, BigDecimal.valueOf(12.00), 5);
        Order buyOrder4 = new Order("user124",
                Type.BUY, BigDecimal.valueOf(12.00), 5);

        Queue<Order> buyOrderQueue = new ArrayDeque<>(List.of(buyOrder1,
                buyOrder2, buyOrder3, buyOrder4));

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.reverseOrder());
        orderBook.put(BigDecimal.valueOf(12.00), buyOrderQueue);

        given(orderRepository.getBidsBook())
                .willReturn(orderBook);

        Status orderStatus = matchingService.matchSell(sellOrder);

        assertEquals(Status.FILLED, orderStatus);
    }

    @Test
    @DisplayName("Test that a SELL order is successfully matched and partially" +
            " filled with a BUY order.")
    public void matchSellPartialFillHappyFlow() {
        Order sellOrder = new Order("user123",
                Type.SELL, BigDecimal.valueOf(12.00), 20);

        Order buyOrder = new Order("user124",
                Type.BUY, BigDecimal.valueOf(12.00), 10);

        Queue<Order> buyOrderQueue = new ArrayDeque<>();
        buyOrderQueue.add(buyOrder);

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.reverseOrder());
        orderBook.put(BigDecimal.valueOf(12.00), buyOrderQueue);

        given(orderRepository.getBidsBook())
                .willReturn(orderBook);

        Status orderStatus = matchingService.matchSell(sellOrder);

        assertEquals(Status.PARTIALLY_FILLED, orderStatus);
    }

    @Test
    @DisplayName("Test that a SELL order is successfully and partially filled" +
            " by multiple BUY orders.")
    public void matchSellMultipleBuyPartialHappyFlow() {
        Order sellOrder = new Order("user123",
                Type.SELL, BigDecimal.valueOf(12.00), 20);

        Order buyOrder1 = new Order("user124",
                Type.BUY, BigDecimal.valueOf(12.00), 5);
        Order buyOrder2 = new Order("user125",
                Type.BUY, BigDecimal.valueOf(12.00), 5);
        Order buyOrder3 = new Order("user126",
                Type.BUY, BigDecimal.valueOf(12.00), 5);

        Queue<Order> buyOrderQueue = new ArrayDeque<>(List.of(buyOrder1,
                buyOrder2, buyOrder3));

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.reverseOrder());
        orderBook.put(BigDecimal.valueOf(12.00), buyOrderQueue);

        given(orderRepository.getBidsBook())
                .willReturn(orderBook);

        Status orderStatus = matchingService.matchSell(sellOrder);

        assertEquals(Status.PARTIALLY_FILLED, orderStatus);
    }

    @Test
    @DisplayName("Test that a SELL order is added to order book when the BUY" +
            " order queue is empty.")
    public void matchSellEmptyBuy() {
        Order sellOrder = new Order("user123",
                Type.SELL, BigDecimal.valueOf(12.00), 20);

        // Empty SELL Queue
        Queue<Order> buyOrderQueue = new ArrayDeque<>();

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.naturalOrder());
        orderBook.put(BigDecimal.valueOf(12.00), buyOrderQueue);

        given(orderRepository.getBidsBook())
                .willReturn(orderBook);

        Status orderStatus = matchingService.matchSell(sellOrder);

        assertEquals(Status.NEW, orderStatus);

        verify(orderRepository).addSellOrder(sellOrder);
    }

    @Test
    @DisplayName("Test that a SELL order is added to order book when the BUY" +
            " order queue is empty.")
    public void matchSellNoBuy() {
        Order sellOrder = new Order("user123",
                Type.SELL, BigDecimal.valueOf(12.00), 20);

        // Empty bids book
        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.naturalOrder());

        given(orderRepository.getBidsBook())
                .willReturn(orderBook);

        Status orderStatus = matchingService.matchSell(sellOrder);

        assertEquals(Status.NEW, orderStatus);

        verify(orderRepository).addSellOrder(sellOrder);
    }

    // Test core matching logic
    @Test
    @DisplayName("Test whether two orders are successfully matched together and" +
            " fully filled when no exception occurs.")
    public void twoOrdersCompletelyMatched() {
        Order restingOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        Order incomingOrder = new Order("user124",
                Type.SELL, BigDecimal.valueOf(12.00), 20);

        Queue<Order> buyOrderQueue = new ArrayDeque<>();
        buyOrderQueue.add(restingOrder);

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.reverseOrder());
        orderBook.put(BigDecimal.valueOf(12.00), buyOrderQueue);

        matchingService.matchOrders(incomingOrder, orderBook);

        assertEquals(0, incomingOrder.getRemainingQuantity());
        assertEquals(Status.FILLED, incomingOrder.getStatus());
        assertEquals(0, restingOrder.getRemainingQuantity());
        assertEquals(Status.FILLED, restingOrder.getStatus());
        verify(settlementService).settle(restingOrder, incomingOrder, 20);
        verify(transactionRepository).addTransaction(
                        restingOrder, incomingOrder, 20);
        assertFalse(buyOrderQueue.contains(restingOrder));
    }

    @Test
    @DisplayName("Test whether an incoming order is partially filled and a" +
            " resting order is fully filled when no exception occurs.")
    public void incomingPartiallyFilledRestingFullyFilled() {
        Order restingOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 10);

        Order incomingOrder = new Order("user124",
                Type.SELL, BigDecimal.valueOf(12.00), 20);

        Queue<Order> buyOrderQueue = new ArrayDeque<>();
        buyOrderQueue.add(restingOrder);

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.reverseOrder());
        orderBook.put(BigDecimal.valueOf(12.00), buyOrderQueue);

        matchingService.matchOrders(incomingOrder, orderBook);

        assertEquals(10, incomingOrder.getRemainingQuantity());
        assertEquals(Status.PARTIALLY_FILLED, incomingOrder.getStatus());
        assertEquals(0, restingOrder.getRemainingQuantity());
        assertEquals(Status.FILLED, restingOrder.getStatus());
        verify(settlementService).settle(restingOrder, incomingOrder, 10);
        verify(transactionRepository).addTransaction(
                restingOrder, incomingOrder, 10);
        assertFalse(buyOrderQueue.contains(restingOrder));
    }

    @Test
    @DisplayName("Test whether an incoming order is fully filled and a" +
            " resting order is partially filled when no exception occurs.")
    public void incomingFullyFilledRestingPartiallyFilled() {
        Order restingOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 30);

        Order incomingOrder = new Order("user124",
                Type.SELL, BigDecimal.valueOf(12.00), 20);

        Queue<Order> buyOrderQueue = new ArrayDeque<>();
        buyOrderQueue.add(restingOrder);

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.reverseOrder());
        orderBook.put(BigDecimal.valueOf(12.00), buyOrderQueue);

        matchingService.matchOrders(incomingOrder, orderBook);

        assertEquals(0, incomingOrder.getRemainingQuantity());
        assertEquals(Status.FILLED, incomingOrder.getStatus());
        assertEquals(10, restingOrder.getRemainingQuantity());
        assertEquals(Status.PARTIALLY_FILLED, restingOrder.getStatus());
        verify(settlementService).settle(restingOrder, incomingOrder, 20);
        verify(transactionRepository).addTransaction(
                restingOrder, incomingOrder, 20);
    }

    @Test
    @DisplayName("Test whether an incoming order is matched properly with" +
            " multiple resting orders, fully filling all the resting orders.")
    public void incomingFullyFilledMultipleRestingFullyFilled() {
        Order restingOrder1 = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 30);
        Order restingOrder2 = new Order("user124",
                Type.BUY, BigDecimal.valueOf(12.00), 60);
        Order restingOrder3 = new Order("user125",
                Type.BUY, BigDecimal.valueOf(12.00), 90);
        Order restingOrder4 = new Order("user126",
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        Order incomingOrder = new Order("user127",
                Type.SELL, BigDecimal.valueOf(12.00), 200);

        Queue<Order> buyOrderQueue = new ArrayDeque<>(
                List.of(restingOrder1,
                        restingOrder2,
                        restingOrder3,
                        restingOrder4));

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.reverseOrder());
        orderBook.put(BigDecimal.valueOf(12.00), buyOrderQueue);

        matchingService.matchOrders(incomingOrder, orderBook);


        assertEquals(0, incomingOrder.getRemainingQuantity());
        assertEquals(Status.FILLED, incomingOrder.getStatus());

        int[] qtyFulfilled = {30, 60, 90, 20};
        Order[] restingOrders = {restingOrder1,
                restingOrder2,
                restingOrder3,
                restingOrder4};
        for(int i = 0; i < 4; i++) {
            assertEquals(0, restingOrders[i].getRemainingQuantity());
            assertEquals(Status.FILLED, restingOrders[i].getStatus());
            verify(settlementService).settle(restingOrders[i],
                    incomingOrder, qtyFulfilled[i]);
            verify(transactionRepository).addTransaction(
                    restingOrders[i], incomingOrder, qtyFulfilled[i]);
        }
    }

    @Test
    @DisplayName("Test whether an incoming order is matched properly with" +
            " multiple resting orders, partially filling some of the resting" +
            " orders.")
    public void incomingFullyFilledMultipleRestingPartiallyFilled() {
        Order restingOrder1 = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 30);
        Order restingOrder2 = new Order("user124",
                Type.BUY, BigDecimal.valueOf(12.00), 60);
        Order restingOrder3 = new Order("user125",
                Type.BUY, BigDecimal.valueOf(12.00), 90);
        Order restingOrder4 = new Order("user126",
                Type.BUY, BigDecimal.valueOf(12.00), 30);

        Order incomingOrder = new Order("user127",
                Type.SELL, BigDecimal.valueOf(12.00), 200);

        Queue<Order> buyOrderQueue = new ArrayDeque<>(
                List.of(restingOrder1,
                        restingOrder2,
                        restingOrder3,
                        restingOrder4));

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.reverseOrder());
        orderBook.put(BigDecimal.valueOf(12.00), buyOrderQueue);

        matchingService.matchOrders(incomingOrder, orderBook);


        assertEquals(0, incomingOrder.getRemainingQuantity());
        assertEquals(Status.FILLED, incomingOrder.getStatus());

        int[] qtyFulfilled = {30, 60, 90, 20};
        Order[] restingOrders = {restingOrder1,
                restingOrder2,
                restingOrder3};
        for(int i = 0; i < 3; i++) {
            assertEquals(0, restingOrders[i].getRemainingQuantity());
            assertEquals(Status.FILLED, restingOrders[i].getStatus());
            verify(settlementService).settle(restingOrders[i],
                    incomingOrder, qtyFulfilled[i]);
            verify(transactionRepository).addTransaction(
                    restingOrders[i], incomingOrder, qtyFulfilled[i]);
        }
        assertEquals(10, restingOrder4.getRemainingQuantity());
        assertEquals(Status.PARTIALLY_FILLED, restingOrder4.getStatus());
        verify(settlementService).settle(restingOrder4,
                incomingOrder, 20);
        verify(transactionRepository).addTransaction(
                restingOrder4, incomingOrder, 20);
    }

    @Test
    @DisplayName("Test whether an incoming order is partially filled by" +
            " multiple resting orders, fully filling all of the resting" +
            " orders.")
    public void incomingPartiallyFilledMultipleRestingFullyFilled() {
        Order restingOrder1 = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 30);
        Order restingOrder2 = new Order("user124",
                Type.BUY, BigDecimal.valueOf(12.00), 60);
        Order restingOrder3 = new Order("user125",
                Type.BUY, BigDecimal.valueOf(12.00), 90);
        Order restingOrder4 = new Order("user126",
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        Order incomingOrder = new Order("user127",
                Type.SELL, BigDecimal.valueOf(12.00), 220);

        Queue<Order> buyOrderQueue = new ArrayDeque<>(
                List.of(restingOrder1,
                        restingOrder2,
                        restingOrder3,
                        restingOrder4));

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.reverseOrder());
        orderBook.put(BigDecimal.valueOf(12.00), buyOrderQueue);

        matchingService.matchOrders(incomingOrder, orderBook);

        assertEquals(20, incomingOrder.getRemainingQuantity());
        assertEquals(Status.PARTIALLY_FILLED, incomingOrder.getStatus());

        int[] qtyFulfilled = {30, 60, 90, 20};
        Order[] restingOrders = {restingOrder1,
                restingOrder2,
                restingOrder3,
                restingOrder4};
        for(int i = 0; i < 4; i++) {
            assertEquals(0, restingOrders[i].getRemainingQuantity());
            assertEquals(Status.FILLED, restingOrders[i].getStatus());
            verify(settlementService).settle(restingOrders[i],
                    incomingOrder, qtyFulfilled[i]);
            verify(transactionRepository).addTransaction(
                    restingOrders[i], incomingOrder, qtyFulfilled[i]);
        }
    }

    @Test
    @DisplayName("Test that a cancelled order is removed from the queue when" +
            " matchingService encounters it.")
    public void cancelledOrderIsRemoved() {
        Order restingOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 10);

        Order incomingOrder = new Order("user124",
                Type.SELL, BigDecimal.valueOf(12.00), 20);

        Queue<Order> buyOrderQueue = new ArrayDeque<>();
        buyOrderQueue.add(restingOrder);

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.reverseOrder());
        orderBook.put(BigDecimal.valueOf(12.00), buyOrderQueue);

        restingOrder.cancel();

        matchingService.matchOrders(incomingOrder, orderBook);

        assertFalse(buyOrderQueue.contains(restingOrder));
    }

    @Test
    @DisplayName("Test whether there is a buyer price improvement" +
            " during an aggressive cross edge.")
    public void aggressiveBuyCrossEdge() {
        Order restingOrder1 = new Order("user1243",
                Type.SELL, BigDecimal.valueOf(100.00), 10);
        // Add other unsuitable orders too to verify cross-edge is handled
        // properly
        Order unsuitableRestingOrder2 = new Order("user1253",
                Type.SELL, BigDecimal.valueOf(105.00), 10);

        Order incomingOrder = new Order("user134",
                Type.BUY, BigDecimal.valueOf(105.00), 10);

        Queue<Order> sellOrderQueue1 = new ArrayDeque<>(List.of(restingOrder1));
        Queue<Order> sellOrderQueue2 = new ArrayDeque<>(
                List.of(unsuitableRestingOrder2));

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.naturalOrder());
        orderBook.put(BigDecimal.valueOf(100.00), sellOrderQueue1);
        orderBook.put(BigDecimal.valueOf(105.00), sellOrderQueue2);

        matchingService.matchOrders(incomingOrder, orderBook);

        assertEquals(0, incomingOrder.getRemainingQuantity());
        assertEquals(Status.FILLED, incomingOrder.getStatus());
        assertEquals(0, restingOrder1.getRemainingQuantity());
        // Verification of cross-edge by checking the resting order's price
        assertEquals(BigDecimal.valueOf(100.00), restingOrder1.getPrice());
        assertEquals(Status.FILLED, restingOrder1.getStatus());
        verify(settlementService).settle(restingOrder1, incomingOrder, 10);
        verify(transactionRepository).addTransaction(
                restingOrder1, incomingOrder, 10);
    }

    @Test
    @DisplayName("Test whether there is a buyer price improvement\" +\n" +
            "            \" during an aggressive cross edge.")
    public void aggressiveSellCrossEdge() {
        Order restingOrder1 = new Order("user1243",
                Type.BUY, BigDecimal.valueOf(100.00), 10);
        // Add other unsuitable orders too to verify cross-edge is handled
        // properly
        Order unsuitableRestingOrder2 = new Order("user1253",
                Type.BUY, BigDecimal.valueOf(95.00), 10);

        Order incomingOrder = new Order("user134",
                Type.SELL, BigDecimal.valueOf(95.00), 10);

        Queue<Order> buyOrderQueue1 = new ArrayDeque<>(List.of(restingOrder1));
        Queue<Order> buyOrderQueue2 = new ArrayDeque<>(
                List.of(unsuitableRestingOrder2));

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.reverseOrder());
        orderBook.put(BigDecimal.valueOf(100.00), buyOrderQueue1);
        orderBook.put(BigDecimal.valueOf(95.00), buyOrderQueue2);

        matchingService.matchOrders(incomingOrder, orderBook);

        assertEquals(0, incomingOrder.getRemainingQuantity());
        assertEquals(Status.FILLED, incomingOrder.getStatus());
        assertEquals(0, restingOrder1.getRemainingQuantity());
        // Verification of cross-edge by checking the resting order's price
        assertEquals(BigDecimal.valueOf(100.00), restingOrder1.getPrice());
        assertEquals(Status.FILLED, restingOrder1.getStatus());
        verify(settlementService).settle(restingOrder1, incomingOrder, 10);
        verify(transactionRepository).addTransaction(
                restingOrder1, incomingOrder, 10);
    }

    @Test
    @DisplayName("Test whether there is a buyer price improvement\" +\n" +
            "            \" during an aggressive cross edge covering orders at" +
            " multiple price levels. The matching should stop when the price " +
            "level goes beyond the incoming order's bounds.")
    public void multiLevelSweepBuyCrossEdgeWithResidualRestingOrder() {
        Order restingOrder1 = new Order("user1243",
                Type.SELL, BigDecimal.valueOf(100.00), 5);
        Order restingOrder2 = new Order("user1253",
                Type.SELL, BigDecimal.valueOf(102.00), 5);
        Order unsuitableRestingOrder3 = new Order("user1464",
                Type.SELL, BigDecimal.valueOf(106.00), 5);

        Order incomingOrder = new Order("user134",
                Type.BUY, BigDecimal.valueOf(105.00), 12);

        Queue<Order> sellOrderQueue1 = new ArrayDeque<>(List.of(restingOrder1));
        Queue<Order> sellOrderQueue2 = new ArrayDeque<>(List.of(restingOrder2));
        Queue<Order> sellOrderQueue3 = new ArrayDeque<>(
                List.of(unsuitableRestingOrder3));

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.naturalOrder());
        orderBook.put(BigDecimal.valueOf(100.00), sellOrderQueue1);
        orderBook.put(BigDecimal.valueOf(102.00), sellOrderQueue2);
        orderBook.put(BigDecimal.valueOf(106.00), sellOrderQueue3);

        matchingService.matchOrders(incomingOrder, orderBook);

        assertEquals(2, incomingOrder.getRemainingQuantity());
        assertEquals(Status.PARTIALLY_FILLED, incomingOrder.getStatus());

        assertEquals(0, restingOrder1.getRemainingQuantity());
        assertEquals(Status.FILLED, restingOrder1.getStatus());

        assertEquals(0, restingOrder2.getRemainingQuantity());
        assertEquals(Status.FILLED, restingOrder2.getStatus());

        assertEquals(5, unsuitableRestingOrder3.getRemainingQuantity());
        assertEquals(Status.NEW, unsuitableRestingOrder3.getStatus());
        assertFalse(sellOrderQueue3.isEmpty());

        verify(settlementService).settle(restingOrder1, incomingOrder, 5);
        verify(settlementService).settle(restingOrder2, incomingOrder, 5);
        verify(settlementService, never()).settle(
                unsuitableRestingOrder3, incomingOrder, 2);
        verify(transactionRepository).addTransaction(
                restingOrder1, incomingOrder, 5);
        verify(transactionRepository).addTransaction(
                restingOrder2, incomingOrder, 5);
        verify(transactionRepository, never()).addTransaction(
                unsuitableRestingOrder3, incomingOrder, 2);
    }

    @Test
    @DisplayName("Test whether there is a seller price improvement\" +\n" +
            "            \" during an aggressive cross edge covering orders at" +
            " multiple price levels. The matching should stop when the price " +
            "level goes beyond the incoming order's bounds.")
    public void multiLevelSweepSellCrossEdgeWithResidualRestingOrder() {
        Order restingOrder1 = new Order("user1243",
                Type.BUY, BigDecimal.valueOf(106.00), 5);
        Order restingOrder2 = new Order("user1253",
                Type.BUY, BigDecimal.valueOf(102.00), 5);
        Order unsuitableRestingOrder3 = new Order("user1464",
                Type.BUY, BigDecimal.valueOf(100.00), 5);

        Order incomingOrder = new Order("user134",
                Type.SELL, BigDecimal.valueOf(102.00), 12);

        Queue<Order> buyOrderQueue1 = new ArrayDeque<>(List.of(restingOrder1));
        Queue<Order> buyOrderQueue2 = new ArrayDeque<>(List.of(restingOrder2));
        Queue<Order> buyOrderQueue3 = new ArrayDeque<>(
                List.of(unsuitableRestingOrder3));

        SortedMap<BigDecimal, Queue<Order>> orderBook =
                new TreeMap<>(Comparator.reverseOrder());
        orderBook.put(BigDecimal.valueOf(106.00), buyOrderQueue1);
        orderBook.put(BigDecimal.valueOf(102.00), buyOrderQueue2);
        orderBook.put(BigDecimal.valueOf(100.00), buyOrderQueue3);

        matchingService.matchOrders(incomingOrder, orderBook);

        assertEquals(2, incomingOrder.getRemainingQuantity());
        assertEquals(Status.PARTIALLY_FILLED, incomingOrder.getStatus());

        assertEquals(0, restingOrder1.getRemainingQuantity());
        assertEquals(Status.FILLED, restingOrder1.getStatus());

        assertEquals(0, restingOrder2.getRemainingQuantity());
        assertEquals(Status.FILLED, restingOrder2.getStatus());

        assertEquals(5, unsuitableRestingOrder3.getRemainingQuantity());
        assertEquals(Status.NEW, unsuitableRestingOrder3.getStatus());

        verify(settlementService).settle(restingOrder1, incomingOrder, 5);
        verify(settlementService).settle(restingOrder2, incomingOrder, 5);
        verify(settlementService, never()).settle(
                unsuitableRestingOrder3, incomingOrder, 2);
        verify(transactionRepository).addTransaction(
                restingOrder1, incomingOrder, 5);
        verify(transactionRepository).addTransaction(
                restingOrder2, incomingOrder, 5);
        verify(transactionRepository, never()).addTransaction(
                unsuitableRestingOrder3, incomingOrder, 2);
    }


}
