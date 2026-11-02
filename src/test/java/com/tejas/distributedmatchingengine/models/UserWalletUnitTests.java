package com.tejas.distributedmatchingengine.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class UserWalletUnitTests {
    private UserWallet userWallet;

    @BeforeEach
    void setup() {
        userWallet = new UserWallet(BigDecimal.valueOf(100.00));
        userWallet.addToAvailableShares(100);
        userWallet.addToTotalShares(100);
    }
    
    @Test
    @DisplayName("Test whether orders are added to the wallet.")
    void addOrdersToList() {
        Order incomingOrder = new Order("user134",
                Type.SELL, BigDecimal.valueOf(95.00), 10);
        userWallet.addOrderToWallet(incomingOrder);
        assertTrue(userWallet.getOrderList().contains(incomingOrder));
    }

    @Test
    @DisplayName("Test whether trying to debit an amount smaller than the" +
            " available balance returns true.")
    void tryDeductFromAvailableBalanceTrue() {
        boolean result = userWallet.tryDeductFromAvailableBalance(
                BigDecimal.valueOf(50.00));
        assertTrue(result);
        assertEquals(0, BigDecimal.valueOf(50.00).compareTo(
                userWallet.getAvailableBalance()));
    }

    @Test
    @DisplayName("Test whether trying to debit an amount larger than the" +
            " available balance returns false.")
    void tryDeductFromAvailableBalanceFalse() {
        boolean result = userWallet.tryDeductFromAvailableBalance(
                BigDecimal.valueOf(150.00));
        assertFalse(result);
        assertEquals(0, BigDecimal.valueOf(100.00).compareTo(
                userWallet.getAvailableBalance()));
    }

    @Test
    @DisplayName("Test whether trying to debit an amount smaller than the" +
            " total balance returns true.")
    void tryDeductFromTotalBalanceTrue() {
        boolean result = userWallet.tryDeductFromTotalBalance(
                BigDecimal.valueOf(50.00));
        assertTrue(result);
        assertEquals(0, BigDecimal.valueOf(50.00).compareTo(
                userWallet.getTotalBalance()));
    }

    @Test
    @DisplayName("Test whether trying to debit an amount larger than the" +
            " available balance returns false.")
    void tryDeductFromTotalBalanceFalse() {
        boolean result = userWallet.tryDeductFromTotalBalance(
                BigDecimal.valueOf(150.00));
        assertFalse(result);
        assertEquals(0, BigDecimal.valueOf(100.00).compareTo(
                userWallet.getTotalBalance()));
    }

    @Test
    @DisplayName("Test whether trying to deduct no. of shares smaller than the" +
            " available shares returns true.")
    void tryDeductFromAvailableSharesTrue() {
        boolean result = userWallet.tryDeductFromAvailableShares(50);
        assertTrue(result);
        assertEquals(50, userWallet.getAvailableShares());
    }

    @Test
    @DisplayName("Test whether trying to debit an amount larger than the" +
            " available balance returns false.")
    void tryDeductFromAvailableSharesFalse() {
        boolean result = userWallet.tryDeductFromAvailableShares(150);
        assertFalse(result);
        assertEquals(100, userWallet.getAvailableShares());
    }

    @Test
    @DisplayName("Test whether trying to deduct no. of shares smaller than the" +
            " available shares returns true.")
    void tryDeductFromTotalSharesTrue() {
        boolean result = userWallet.tryDeductFromTotalShares(50);
        assertTrue(result);
        assertEquals(50, userWallet.getTotalShares());
    }

    @Test
    @DisplayName("Test whether trying to debit an amount larger than the" +
            " available balance returns false.")
    void tryDeductFromTotalSharesFalse() {
        boolean result = userWallet.tryDeductFromTotalShares(150);
        assertFalse(result);
        assertEquals(100, userWallet.getTotalShares());
    }
}
