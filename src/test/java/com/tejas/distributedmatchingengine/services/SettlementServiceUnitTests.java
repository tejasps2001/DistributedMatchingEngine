package com.tejas.distributedmatchingengine.services;

import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.Type;
import com.tejas.distributedmatchingengine.models.UserWallet;
import com.tejas.distributedmatchingengine.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceUnitTests {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SettlementService settlementService;

    @Test
    @DisplayName("Test whether a resting BUY and an incoming SELL are properly" +
            " settled.")
    void settleRestingBuyIncomingSell() {
        // Spying on the tested object since settle() returns nothing and
        // the method calls inside the settle() cannot be stubbed (since they are
        // part of the real object). The only option left is to spy.
        SettlementService spiedSettlementService =
                spy(new SettlementService(userRepository));

        Order restingBuyOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        Order incomingSellOrder = new Order("user124",
                Type.SELL, BigDecimal.valueOf(11.00), 20);

        // Do nothing when the actual methods are called by the spied object
        // since only the settle() is being tested right now
        doNothing().when(spiedSettlementService).settleBuy(any(), any(), anyInt());
        doNothing().when(spiedSettlementService).settleSell(any(), any(), anyInt());

        spiedSettlementService.settle(restingBuyOrder,
                incomingSellOrder, 20);

        // Timeout set so that verify waits until the asynchronous executor picks
        // up the task and executes the below method calls.
        verify(spiedSettlementService, timeout(1000)).settleBuy(restingBuyOrder,
                restingBuyOrder.getPrice(), 20);
        verify(spiedSettlementService, timeout(1000)).settleSell(incomingSellOrder,
                restingBuyOrder.getPrice(), 20);
    }

    @Test
    @DisplayName("Test whether a resting SELL and an incoming BUY are properly" +
            " settled.")
    void settleRestingSellIncomingBuy() {
        // Spying on the tested object since settle() returns nothing and
        // the method calls inside the settle() cannot be stubbed (since they are
        // part of the real object). The only option left is to spy.
        SettlementService spiedSettlementService =
                spy(new SettlementService(userRepository));

        Order incomingBuyOrder = new Order("user123",
                Type.BUY, BigDecimal.valueOf(13.00), 20);

        Order restingSellOrder = new Order("user124",
                Type.SELL, BigDecimal.valueOf(12.00), 20);

        // Do nothing when the actual methods are called by the spied object
        // since only the settle() is being tested right now
        doNothing().when(spiedSettlementService).settleBuy(any(), any(), anyInt());
        doNothing().when(spiedSettlementService).settleSell(any(), any(), anyInt());

        spiedSettlementService.settle(restingSellOrder,
                incomingBuyOrder, 20);

        // Timeout set so that verify waits until the asynchronous executor picks
        // up the task and executes the below method calls.
        verify(spiedSettlementService, timeout(1000)).settleBuy(incomingBuyOrder,
                restingSellOrder.getPrice(), 20);
        verify(spiedSettlementService, timeout(1000)).settleSell(restingSellOrder,
                restingSellOrder.getPrice(), 20);
    }

    @Test
    @DisplayName("Test that a partially filled sell order is correctly settled.")
    void settlePartiallyFilledSell() {
        UserWallet wallet = new UserWallet(BigDecimal.valueOf(14.00));
        wallet.addToTotalShares(20);

        Order sellOrder = new Order(wallet.getUserID(),
                Type.SELL, BigDecimal.valueOf(11.00), 20);

        // When a sell order is sent to the orderController, it ties up the
        // allocated shares by deducting from availableShares. Hence, set to
        // 0 below
        wallet.addToAvailableShares(0);

        sellOrder.setStatus(Status.PARTIALLY_FILLED);

        given(userRepository.findUserByID(wallet.getUserID())).
                willReturn(Optional.of(wallet));

        settlementService.settleSell(sellOrder,
                BigDecimal.valueOf(11.00), 10);

        assertEquals(10, wallet.getTotalShares());
        assertEquals(BigDecimal.valueOf(124.00), wallet.getAvailableBalance());
        assertEquals(BigDecimal.valueOf(124.00), wallet.getTotalBalance());
    }

    @Test
    @DisplayName("Test that a fully filled sell order is correctly settled.")
    void settleFullyFilledSell() {
        UserWallet wallet = new UserWallet(BigDecimal.valueOf(14.00));
        wallet.addToTotalShares(20);

        Order sellOrder = new Order(wallet.getUserID(),
                Type.SELL, BigDecimal.valueOf(11.00), 20);

        // When a sell order is sent to the orderController, it ties up the
        // allocated shares by deducting from availableShares. Hence, set to
        // 0 below
        wallet.addToAvailableShares(0);

        sellOrder.setStatus(Status.FILLED);

        given(userRepository.findUserByID(wallet.getUserID())).
                willReturn(Optional.of(wallet));

        settlementService.settleSell(sellOrder,
                BigDecimal.valueOf(11.00), 20);

        assertEquals(0, wallet.getTotalShares());
        assertEquals(BigDecimal.valueOf(234.00), wallet.getAvailableBalance());
        assertEquals(BigDecimal.valueOf(234.00), wallet.getTotalBalance());
    }

    @Test
    @DisplayName("Test that an aggressive partially filled sell order is" +
            " correctly settled.")
    void settleAggressivePartiallyFilledSell() {
        UserWallet wallet = new UserWallet(BigDecimal.valueOf(14.00));
        wallet.addToTotalShares(20);

        Order sellOrder = new Order(wallet.getUserID(),
                Type.SELL, BigDecimal.valueOf(11.00), 20);

        // When a sell order is sent to the orderController, it ties up the
        // allocated shares by deducting from availableShares. Hence, set to
        // 0 below
        wallet.addToAvailableShares(0);

        sellOrder.setStatus(Status.PARTIALLY_FILLED);

        given(userRepository.findUserByID(wallet.getUserID())).
                willReturn(Optional.of(wallet));

        settlementService.settleSell(sellOrder,
                BigDecimal.valueOf(15.00), 10);

        assertEquals(10, wallet.getTotalShares());
        assertEquals(BigDecimal.valueOf(164.00), wallet.getAvailableBalance());
        assertEquals(BigDecimal.valueOf(164.00), wallet.getTotalBalance());
    }

    @Test
    @DisplayName("Test that an aggressive fully filled sell order is" +
            " correctly settled.")
    void settleAggressiveFullyFilledSell() {
        UserWallet wallet = new UserWallet(BigDecimal.valueOf(14.00));
        wallet.addToTotalShares(20);

        Order sellOrder = new Order(wallet.getUserID(),
                Type.SELL, BigDecimal.valueOf(11.00), 20);

        // When a sell order is sent to the orderController, it ties up the
        // allocated shares by deducting from availableShares. Hence, set to
        // 0 below
        wallet.addToAvailableShares(0);

        sellOrder.setStatus(Status.FILLED);

        given(userRepository.findUserByID(wallet.getUserID())).
                willReturn(Optional.of(wallet));

        settlementService.settleSell(sellOrder,
                BigDecimal.valueOf(15.00), 20);

        assertEquals(0, wallet.getTotalShares());
        assertEquals(BigDecimal.valueOf(314.00), wallet.getAvailableBalance());
        assertEquals(BigDecimal.valueOf(314.00), wallet.getTotalBalance());
    }

    @Test
    @DisplayName("Test that a partially filled buy order is correctly settled.")
    void settlePartiallyFilledBuy() {
        UserWallet wallet = new UserWallet(BigDecimal.valueOf(114.00));

        Order buyOrder = new Order(wallet.getUserID(),
                Type.BUY, BigDecimal.valueOf(11.00), 10);
        buyOrder.setStatus(Status.PARTIALLY_FILLED);

        given(userRepository.findUserByID(wallet.getUserID())).
                willReturn(Optional.of(wallet));

        settlementService.settleBuy(buyOrder,
                BigDecimal.valueOf(11.00), 5);

        assertEquals(5, wallet.getAvailableShares());
        assertEquals(5, wallet.getTotalShares());
        assertEquals(BigDecimal.valueOf(59.00), wallet.getTotalBalance());
    }

    @Test
    @DisplayName("Test that a fully filled buy order is correctly settled.")
    void settleFullyFilledBuy() {
        UserWallet wallet = new UserWallet(BigDecimal.valueOf(114.00));

        Order buyOrder = new Order(wallet.getUserID(),
                Type.BUY, BigDecimal.valueOf(11.00), 10);
        buyOrder.setStatus(Status.FILLED);

        given(userRepository.findUserByID(wallet.getUserID())).
                willReturn(Optional.of(wallet));

        settlementService.settleBuy(buyOrder,
                BigDecimal.valueOf(11.00), 10);

        assertEquals(10, wallet.getAvailableShares());
        assertEquals(10, wallet.getTotalShares());
        assertEquals(BigDecimal.valueOf(4.00), wallet.getTotalBalance());
    }

    @Test
    @DisplayName("Test that an aggressive partially filled buy order is" +
            " correctly settled.")
    void settleAggressivePartiallyFilledBuy() {
        UserWallet wallet = new UserWallet(BigDecimal.valueOf(114.00));

        Order buyOrder = new Order(wallet.getUserID(),
                Type.BUY, BigDecimal.valueOf(11.00), 10);

        // Deduct from available balance since placing the order ties up money
        wallet.tryDeductFromAvailableBalance(BigDecimal.valueOf(110.00));

        buyOrder.setStatus(Status.PARTIALLY_FILLED);

        given(userRepository.findUserByID(wallet.getUserID())).
                willReturn(Optional.of(wallet));

        settlementService.settleBuy(buyOrder,
                BigDecimal.valueOf(8.00), 5);

        assertEquals(5, wallet.getAvailableShares());
        assertEquals(5, wallet.getTotalShares());
        assertEquals(BigDecimal.valueOf(74.00), wallet.getTotalBalance());
        assertEquals(BigDecimal.valueOf(19.00), wallet.getAvailableBalance());
    }

    @Test
    @DisplayName("Test that an aggressive fully filled buy order is" +
            " correctly settled.")
    void settleAggressiveFullyFilledBuy() {
        UserWallet wallet = new UserWallet(BigDecimal.valueOf(114.00));

        Order buyOrder = new Order(wallet.getUserID(),
                Type.BUY, BigDecimal.valueOf(11.00), 10);

        // Deduct from available balance since placing the order ties up money
        wallet.tryDeductFromAvailableBalance(BigDecimal.valueOf(110.00));

        buyOrder.setStatus(Status.FILLED);

        given(userRepository.findUserByID(wallet.getUserID())).
                willReturn(Optional.of(wallet));

        settlementService.settleBuy(buyOrder,
                BigDecimal.valueOf(8.00), 10);

        assertEquals(10, wallet.getAvailableShares());
        assertEquals(10, wallet.getTotalShares());
        assertEquals(BigDecimal.valueOf(34.00), wallet.getTotalBalance());
        assertEquals(BigDecimal.valueOf(34.00), wallet.getAvailableBalance());
    }

    @Test
    @DisplayName("Test that a cancelled buy order is correctly settled.")
    void settleCancelledBuy() throws InterruptedException {
        UserWallet wallet = new UserWallet(BigDecimal.valueOf(114.00));


        Order buyOrder = new Order(wallet.getUserID(),
                Type.BUY, BigDecimal.valueOf(11.00), 10);

        // Deduct from available balance since placing the order ties up money
        wallet.tryDeductFromAvailableBalance(BigDecimal.valueOf(110.00));

        given(userRepository.findUserByID(wallet.getUserID())).
                willReturn(Optional.of(wallet));

        settlementService.settleCancellation(buyOrder);

        Thread.sleep(1000);
        assertEquals(BigDecimal.valueOf(114.00), wallet.getAvailableBalance());
    }

    @Test
    @DisplayName("Test that a cancelled sell order is correctly settled.")
    void settleCancelledSell() throws InterruptedException {
        UserWallet wallet = new UserWallet(BigDecimal.valueOf(114.00));
        wallet.addToTotalShares(10);

        Order sellOrder = new Order(wallet.getUserID(),
                Type.SELL, BigDecimal.valueOf(11.00), 10);

        // When a sell order is sent to the orderController, it ties up the
        // allocated shares by deducting from availableShares. Hence, set to
        // 0 below
        wallet.addToAvailableShares(0);

        given(userRepository.findUserByID(wallet.getUserID())).
                willReturn(Optional.of(wallet));

        settlementService.settleCancellation(sellOrder);

        Thread.sleep(1000);
        assertEquals(10, wallet.getAvailableShares());
    }
}
