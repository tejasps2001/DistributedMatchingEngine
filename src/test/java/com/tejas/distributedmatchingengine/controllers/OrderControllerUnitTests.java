package com.tejas.distributedmatchingengine.controllers;

import com.tejas.distributedmatchingengine.exceptions.UserNotFoundException;
import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.Type;
import com.tejas.distributedmatchingengine.models.UserWallet;
import com.tejas.distributedmatchingengine.repositories.UserRepository;
import com.tejas.distributedmatchingengine.services.MatchingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

// Using package-private access since JUnit 5 can run these tests using reflection
@ExtendWith(MockitoExtension.class)
class OrderControllerUnitTests {
    @Mock
    private MatchingService matchingService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderController orderController;

    // Tests for the sell controller action
    @Test
    @DisplayName("Test whether a SELL order is successfully matched and fully" +
            " filled.")
    void sellOrderFullPlacementHappyFlow() {
        // Assumptions
        // Set up selling user's wallet
        UserWallet sellingWallet = new UserWallet(BigDecimal.valueOf(100.00));
        String sellingWalletUserID = sellingWallet.getUserID();
        sellingWallet.addToTotalShares(20);
        sellingWallet.addToAvailableShares(20);

        given(userRepository.findUserByID(sellingWalletUserID))
                .willReturn(Optional.of(sellingWallet));

        Order sellOrder = new Order(sellingWalletUserID,
                Type.SELL, BigDecimal.valueOf(24.00), 10);

        given(matchingService.matchSell(sellOrder))
                .willReturn(Status.FILLED);

        // Call/Executions
        var orderStatus = orderController.placeSell(sellOrder);

        // Validations
        assertEquals("Order executed.", orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether a SELL order is successfully matched and partially" +
            " filled.")
    void sellOrderPartialPlacementHappyFlow() {
        // Assumptions
        // Set up selling user's wallet
        UserWallet sellingWallet = new UserWallet(BigDecimal.valueOf(100.00));
        String sellingWalletUserID = sellingWallet.getUserID();
        sellingWallet.addToTotalShares(20);
        sellingWallet.addToAvailableShares(20);

        given(userRepository.findUserByID(sellingWalletUserID))
                .willReturn(Optional.of(sellingWallet));

        Order sellOrder = new Order(sellingWalletUserID,
                Type.SELL, BigDecimal.valueOf(24.00), 10);

        given(matchingService.matchSell(sellOrder))
                .willReturn(Status.PARTIALLY_FILLED);

        // Call/Executions
        var orderStatus = orderController.placeSell(sellOrder);

        // Validations
        assertEquals(String.format("""
                        Filled %d shares.""",
                sellOrder.getQuantity() - sellOrder.getRemainingQuantity()),
                orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether a SELL order is not matched.")
    void sellOrderNoPlacementHappyFlow() {
        // Assumptions
        // Set up selling user's wallet
        UserWallet sellingWallet = new UserWallet(BigDecimal.valueOf(100.00));
        String sellingWalletUserID = sellingWallet.getUserID();
        sellingWallet.addToTotalShares(20);
        sellingWallet.addToAvailableShares(20);

        given(userRepository.findUserByID(sellingWalletUserID))
                .willReturn(Optional.of(sellingWallet));

        Order sellOrder = new Order(sellingWalletUserID,
                Type.SELL, BigDecimal.valueOf(24.00), 10);

        given(matchingService.matchSell(sellOrder))
                .willReturn(Status.NEW);

        // Call/Executions
        var orderStatus = orderController.placeSell(sellOrder);

        // Validations
        assertEquals("Couldn't fill order. Added to order book.",
                orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether a SELL order is not matched when the user has " +
            "insufficient amount of shares")
    void sellOrderInsufficientShares() {
        // Assumptions
        // Set up selling user's wallet
        UserWallet sellingWallet = new UserWallet(BigDecimal.valueOf(100.00));
        String sellingWalletUserID = sellingWallet.getUserID();
        sellingWallet.addToTotalShares(20);
        sellingWallet.addToAvailableShares(20);

        given(userRepository.findUserByID(sellingWalletUserID))
                .willReturn(Optional.of(sellingWallet));

        Order sellOrder = new Order(sellingWalletUserID,
                Type.SELL, BigDecimal.valueOf(24.00), 100);

        // Call/Executions
        var orderStatus = orderController.placeSell(sellOrder);

        // Validations
        assertEquals("Insufficient amount of shares!",
                orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether an exception is thrown when there is no entry for" +
            "the userID in the UserRespository.")
    void sellingUserDoesNotExist() {
        Order sellOrder = new Order("user132",
                Type.SELL, BigDecimal.valueOf(24.00), 10);

        assertThrows(UserNotFoundException.class,
                () -> orderController.placeSell(sellOrder));
    }

    // Tests for the buy controller action
    @Test
    @DisplayName("Test whether a BUY order is successfully matched and fully" +
            " filled.")
    void buyOrderFullPlacementHappyFlow() {
        // Assumptions
        // Set up buying user's wallet
        UserWallet buyingWallet = new UserWallet(BigDecimal.valueOf(2500.00));
        String buyingWalletUserID = buyingWallet.getUserID();

        given(userRepository.findUserByID(buyingWalletUserID))
                .willReturn(Optional.of(buyingWallet));

        Order buyOrder = new Order(buyingWalletUserID,
                Type.BUY, BigDecimal.valueOf(24.00), 10);

        given(matchingService.matchBuy(buyOrder))
                .willReturn(Status.FILLED);

        // Call/Executions
        var orderStatus = orderController.placeBuy(buyOrder);

        // Validations
        assertEquals("Order executed.", orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether a BUY order is successfully matched and partially" +
            " filled.")
    void buyOrderPartialPlacementHappyFlow() {
        // Assumptions
        // Set up buying user's wallet
        UserWallet buyingWallet = new UserWallet(BigDecimal.valueOf(2500.00));
        String buyingWalletUserID = buyingWallet.getUserID();

        given(userRepository.findUserByID(buyingWalletUserID))
                .willReturn(Optional.of(buyingWallet));

        Order buyOrder = new Order(buyingWalletUserID,
                Type.BUY, BigDecimal.valueOf(24.00), 10);

        given(matchingService.matchBuy(buyOrder))
                .willReturn(Status.PARTIALLY_FILLED);

        // Call/Executions
        var orderStatus = orderController.placeBuy(buyOrder);

        // Validations
        assertEquals(String.format("""
                        Bought %d shares.""",
                        buyOrder.getQuantity() - buyOrder.getRemainingQuantity()),
                orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether a BUY order is not matched.")
    void buyOrderNoPlacementHappyFlow() {
        // Assumptions
        // Set up buying user's wallet
        UserWallet buyingWallet = new UserWallet(BigDecimal.valueOf(5000.00));
        String buyingWalletUserID = buyingWallet.getUserID();

        given(userRepository.findUserByID(buyingWalletUserID))
                .willReturn(Optional.of(buyingWallet));

        Order buyOrder = new Order(buyingWalletUserID,
                Type.BUY, BigDecimal.valueOf(24.00), 10);

        given(matchingService.matchBuy(buyOrder))
                .willReturn(Status.NEW);

        // Call/Executions
        var orderStatus = orderController.placeBuy(buyOrder);

        // Validations
        assertEquals("Couldn't fill order. Added to order book.",
                orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether a BUY order is not matched when the user has " +
            "insufficient money.")
    void buyOrderInsufficientMoney() {
        // Assumptions
        // Set up buying user's wallet
        UserWallet buyingWallet = new UserWallet(BigDecimal.valueOf(100.00));
        String buyingWalletUserID = buyingWallet.getUserID();

        given(userRepository.findUserByID(buyingWalletUserID))
                .willReturn(Optional.of(buyingWallet));

        Order buyOrder = new Order(buyingWalletUserID,
                Type.BUY, BigDecimal.valueOf(24.00), 10);

        // Call/Executions
        var orderStatus = orderController.placeBuy(buyOrder);

        // Validations
        assertEquals("Insufficient funds!", orderStatus.getBody());
    }

    @Test
    @DisplayName("Test whether an exception is thrown when there is no entry for" +
            "the userID in the UserRespository.")
    void buyingUserDoesNotExist() {
        Order sellOrder = new Order("user132",
                Type.SELL, BigDecimal.valueOf(24.00), 10);

        assertThrows(UserNotFoundException.class,
                () -> orderController.placeSell(sellOrder));
    }

    // Tests for the cancel controller action
    @Test
    @DisplayName("Test that the order is cancelled.")
    void orderCancellationHappyFlow() {
        Order cancelOrder = new Order("user132",
                Type.SELL, BigDecimal.valueOf(24.00), 10);

        cancelOrder.setStatus(Status.CANCELLED);

        var cancelStatus = orderController.cancelOrder(cancelOrder);

        assertEquals("Order cancelled.", cancelStatus.getBody());
    }

    @Test
    @DisplayName("Test that the order is not cancelled when it is already" +
            " filled.")
    void orderCancellationDeniedAlreadyFilled() {
        Order cancelOrder = new Order("user132",
                Type.SELL, BigDecimal.valueOf(24.00), 10);

        cancelOrder.setStatus(Status.FILLED);

        var cancelStatus = orderController.cancelOrder(cancelOrder);

        assertEquals("Couldn't cancel. Order already filled.",
                cancelStatus.getBody());
    }
}
