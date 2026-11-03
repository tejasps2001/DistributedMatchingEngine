package com.tejas.distributedmatchingengine.controllers;

import com.tejas.distributedmatchingengine.exceptions.UserNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserControllerUnitTests {
    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserController userController;

    @Test
    @DisplayName("Test that the portfolio is displayed.")
    void getPortfolioHappyFlow() {
        UserWallet sellingWallet = new UserWallet(BigDecimal.valueOf(100.00));
        String sellingWalletUserID = sellingWallet.getUserID();
        sellingWallet.addToTotalShares(20);
        sellingWallet.addToAvailableShares(20);

        given(userRepository.findUserByID(sellingWalletUserID))
                .willReturn(Optional.of(sellingWallet));

        var portfolio = userController.getWallet(sellingWalletUserID);

        assert portfolio.getBody() != null;
        assertEquals(sellingWallet.toString(), portfolio.getBody().toString());
    }

    @Test
    @DisplayName("Test that an exception is thrown when there is no user with" +
            "the given userID.")
    void getPortfolioUserNotFound() {
        given(userRepository.findUserByID("user123"))
                .willReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userController.getWallet("user123"));
    }

    @Test
    @DisplayName("Test that a new user is created.")
    void createNewUserHappyFlow() {
        userController.createNewUser(1000.00);
        verify(userRepository)
                .addUser(BigDecimal.valueOf(1000.00));
    }

    @Test
    @DisplayName("Test that a new user is not created if a negative balance " +
            "is given.")
    void doNotCreateUserIfNegativeBalanceGiven() {
        var result = userController.createNewUser(-100.00);
        assertEquals("Negative balance not allowed!", result.getBody());
    }

    @Test
    @DisplayName("Test that a new user with shares is created.")
    void createNewUserWithSharesHappyFlow() {
        userController.createNewUserWithShares(1000.00, 1000);
        verify(userRepository)
                .addUserWithShares(BigDecimal.valueOf(1000.00), 1000);
    }

    @Test
    @DisplayName("Test that a new user is not created if a negative balance " +
            "is given.")
    void doNotCreateUserWithSharesIfNegativeBalanceGiven() {
        var result = userController.createNewUserWithShares(
                -100.00, 100);
        assertEquals("Negative balance not allowed!", result.getBody());
    }

    @Test
    @DisplayName("Test that a new user is not created if a negative amount " +
            "of shares is given.")
    void doNotCreateUserWithSharesIfNegativeSharesGiven() {
        var result = userController.createNewUserWithShares(
                100.00, -100);
        assertEquals("Number of shares can't be less than zero!",
                result.getBody());
    }
}
