package com.tejas.distributedmatchingengine.controllers;

import com.tejas.distributedmatchingengine.DTOs.OrderMatchResponse;
import com.tejas.distributedmatchingengine.DTOs.PlaceOrderRequest;
import com.tejas.distributedmatchingengine.exceptions.UserNotFoundException;
import com.tejas.distributedmatchingengine.models.*;
import com.tejas.distributedmatchingengine.repositories.UserRepository;
import com.tejas.distributedmatchingengine.services.MatchingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

// Using package-private access since it would allow JUnit 5 to run these
// tests using reflection
@ExtendWith(MockitoExtension.class)
class OrderControllerUnitTests {
    @Mock
    private MatchingService matchingService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderController orderController;

    BigDecimal orderPrice = BigDecimal.valueOf(24.00);

    @Test
    @DisplayName("Test whether a SELL order is successfully matched and fully" +
            " filled.")
    void sellOrderFullPlacementHappyFlow() {
        UserWallet sellingWallet = new UserWallet(BigDecimal.valueOf(100.00));
        String sellingWalletUserID = sellingWallet.getUserID();
        sellingWallet.addToTotalShares(20);
        sellingWallet.addToAvailableShares(20);

        given(userRepository.findUserByID(sellingWalletUserID))
                .willReturn(Optional.of(sellingWallet));

        // 'matches' can be null since it is not the focus of the test
        OrderMatchResponse response = new OrderMatchResponse(
                Status.FILLED,
                null);

        given(matchingService.matchSell(any()))
                .willReturn(response);

        PlaceOrderRequest sellOrderDTO = new PlaceOrderRequest(
                sellingWalletUserID, orderPrice, 10
        );

        var orderStatus = orderController.placeSell(sellOrderDTO);

        assertEquals("Order executed.", orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether a SELL order is successfully matched and partially" +
            " filled.")
    void sellOrderPartialPlacementHappyFlow() {
        UserWallet sellingWallet = new UserWallet(BigDecimal.valueOf(100.00));
        String sellingWalletUserID = sellingWallet.getUserID();
        sellingWallet.addToTotalShares(20);
        sellingWallet.addToAvailableShares(20);

        given(userRepository.findUserByID(sellingWalletUserID))
                .willReturn(Optional.of(sellingWallet));

        // 'matches' can be null since it is not the focus of the test
        OrderMatchResponse response = new OrderMatchResponse(
                Status.PARTIALLY_FILLED,
                List.of(new Transaction(1,
                        Instant.now(),
                        1,
                        2,
                        BigDecimal.ZERO,
                        5)));

        given(matchingService.matchSell(any()))
                .willReturn(response);

        PlaceOrderRequest sellOrderDTO = new PlaceOrderRequest(
                sellingWalletUserID, orderPrice, 10
        );

        var orderStatus = orderController.placeSell(sellOrderDTO);

        assertEquals("Filled 5 shares.", orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether a SELL order is not matched.")
    void sellOrderNoPlacementHappyFlow() {
        UserWallet sellingWallet = new UserWallet(BigDecimal.valueOf(100.00));
        String sellingWalletUserID = sellingWallet.getUserID();
        sellingWallet.addToTotalShares(20);
        sellingWallet.addToAvailableShares(20);

        given(userRepository.findUserByID(sellingWalletUserID))
                .willReturn(Optional.of(sellingWallet));

        // 'matches' can be null since it is not the focus of the test
        OrderMatchResponse response = new OrderMatchResponse(
                Status.NEW,
                null);

        given(matchingService.matchSell(any()))
                .willReturn(response);

        PlaceOrderRequest sellOrderDTO = new PlaceOrderRequest(
                sellingWalletUserID, orderPrice, 10
        );

        var orderStatus = orderController.placeSell(sellOrderDTO);

        assertEquals("Couldn't fill order. Added to order book.",
                orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether a SELL order is not matched when the user has " +
            "insufficient amount of shares")
    void sellOrderInsufficientShares() {
        UserWallet sellingWallet = new UserWallet(BigDecimal.valueOf(100.00));
        String sellingWalletUserID = sellingWallet.getUserID();
        sellingWallet.addToTotalShares(20);
        sellingWallet.addToAvailableShares(20);

        given(userRepository.findUserByID(sellingWalletUserID))
                .willReturn(Optional.of(sellingWallet));

        PlaceOrderRequest sellOrderDTO = new PlaceOrderRequest(
                sellingWalletUserID, orderPrice, 100
        );

        var orderStatus = orderController.placeSell(sellOrderDTO);

        assertEquals("Insufficient amount of shares!",
                orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether an exception is thrown when there is no entry for" +
            "the userID in the UserRepository.")
    void sellingUserDoesNotExist() {
        PlaceOrderRequest sellOrderDTO = new PlaceOrderRequest(
                "user132", orderPrice, 10
        );

        assertThrows(UserNotFoundException.class,
                () -> orderController.placeSell(sellOrderDTO));
    }

    @Test
    @DisplayName("Test whether a BUY order is successfully matched and fully" +
            " filled.")
    void buyOrderFullPlacementHappyFlow() {
        UserWallet buyingWallet = new UserWallet(BigDecimal.valueOf(2500.00));
        String buyingWalletUserID = buyingWallet.getUserID();

        given(userRepository.findUserByID(buyingWalletUserID))
                .willReturn(Optional.of(buyingWallet));

        // 'matches' can be null since it is not the focus of the test
        OrderMatchResponse response = new OrderMatchResponse(
                Status.FILLED,
                null);

        given(matchingService.matchBuy(any()))
                .willReturn(response);

        PlaceOrderRequest buyOrderDTO = new PlaceOrderRequest(
                buyingWalletUserID, orderPrice, 10
        );

        var orderStatus = orderController.placeBuy(buyOrderDTO);

        assertEquals("Order executed.", orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether a BUY order is successfully matched and partially" +
            " filled.")
    void buyOrderPartialPlacementHappyFlow() {
        UserWallet buyingWallet = new UserWallet(BigDecimal.valueOf(2500.00));
        String buyingWalletUserID = buyingWallet.getUserID();

        given(userRepository.findUserByID(buyingWalletUserID))
                .willReturn(Optional.of(buyingWallet));

        OrderMatchResponse response = new OrderMatchResponse(
                Status.PARTIALLY_FILLED,
                List.of(new Transaction(1,
                        Instant.now(),
                        1,
                        2,
                        BigDecimal.ZERO,
                        5)));

        given(matchingService.matchBuy(any()))
                .willReturn(response);

        PlaceOrderRequest buyOrderDTO = new PlaceOrderRequest(
                buyingWalletUserID, orderPrice, 10
        );

        var orderStatus = orderController.placeBuy(buyOrderDTO);

        assertEquals("Bought 5 shares.", orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether a BUY order is not matched.")
    void buyOrderNoPlacementHappyFlow() {
        UserWallet buyingWallet = new UserWallet(BigDecimal.valueOf(5000.00));
        String buyingWalletUserID = buyingWallet.getUserID();

        given(userRepository.findUserByID(buyingWalletUserID))
                .willReturn(Optional.of(buyingWallet));

        // 'matches' can be null since it is not the focus of the test
        OrderMatchResponse response = new OrderMatchResponse(
                Status.NEW,
                null);

        given(matchingService.matchBuy(any()))
                .willReturn(response);

        PlaceOrderRequest buyOrderDTO = new PlaceOrderRequest(
                buyingWalletUserID, orderPrice, 10
        );

        var orderStatus = orderController.placeBuy(buyOrderDTO);

        assertEquals("Couldn't fill order. Added to order book.",
                orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether a BUY order is not matched when the user has " +
            "insufficient money.")
    void buyOrderInsufficientMoney() {
        UserWallet buyingWallet = new UserWallet(BigDecimal.valueOf(100.00));
        String buyingWalletUserID = buyingWallet.getUserID();

        given(userRepository.findUserByID(buyingWalletUserID))
                .willReturn(Optional.of(buyingWallet));

        PlaceOrderRequest buyOrderDTO = new PlaceOrderRequest(
                buyingWalletUserID, orderPrice, 10
        );

        var orderStatus = orderController.placeBuy(buyOrderDTO);

        assertEquals("Insufficient funds!", orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether an exception is thrown when there is no entry for" +
            "the userID in the UserRepository.")
    void buyingUserDoesNotExist() {
        PlaceOrderRequest buyOrderDTO = new PlaceOrderRequest(
                "user132", orderPrice, 10
        );

        assertThrows(UserNotFoundException.class,
                () -> orderController.placeBuy(buyOrderDTO));
    }

    @Test
    @DisplayName("Test that the order is cancelled.")
    void orderCancellationHappyFlow() {
        Order cancelOrder = new Order("user132",
                Type.SELL, orderPrice, 10);
        long orderID = cancelOrder.getOrderId();
        cancelOrder.setStatus(Status.CANCELLED);
        given(matchingService.cancelOrder(orderID))
                .willReturn(cancelOrder.getStatus());

        var cancelStatus = orderController.cancelOrder(orderID);

        assertEquals("Order cancelled.", cancelStatus.getBody());
    }

    @Test
    @DisplayName("Test that the order is not cancelled when it is already" +
            " filled.")
    void orderCancellationDeniedAlreadyFilled() {
        Order cancelOrder = new Order("user132",
                Type.SELL, orderPrice, 10);
        long orderID = cancelOrder.getOrderId();
        cancelOrder.setStatus(Status.FILLED);
        given(matchingService.cancelOrder(orderID))
                .willReturn(cancelOrder.getStatus());

        var cancelStatus = orderController.cancelOrder(orderID);

        assertEquals("Couldn't cancel. Order already filled.",
                cancelStatus.getBody());
    }
}
