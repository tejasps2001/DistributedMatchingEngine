package com.tejas.distributedmatchingengine.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class UserWalletUnitTests {
    private UserWallet userWallet;

    @BeforeEach
    void setup() {
        userWallet = new UserWallet(BigDecimal.valueOf(100.00));
        userWallet.addToAvailableShares(100);
        userWallet.addToTotalShares(100);
    }

    @Test
    @DisplayName("Test whether trying to debit an amount smaller than the" +
            " available balance returns true.")
    public void tryDeductFromAvailableBalanceTrue() {
        boolean result = userWallet.tryDeductFromAvailableBalance(
                BigDecimal.valueOf(50.00));
        assertTrue(result);
        assertEquals(0, BigDecimal.valueOf(50.00).compareTo(
                userWallet.getAvailableBalance()));
    }

    @Test
    @DisplayName("Test whether trying to debit an amount larger than the" +
            " available balance returns false.")
    public void tryDeductFromAvailableBalanceFalse() {
        boolean result = userWallet.tryDeductFromAvailableBalance(
                BigDecimal.valueOf(150.00));
        assertFalse(result);
        assertEquals(0, BigDecimal.valueOf(100.00).compareTo(
                userWallet.getAvailableBalance()));
    }

    @Test
    @DisplayName("Test whether trying to debit an amount smaller than the" +
            " total balance returns true.")
    public void tryDeductFromTotalBalanceTrue() {
        boolean result = userWallet.tryDeductFromTotalBalance(
                BigDecimal.valueOf(50.00));
        assertTrue(result);
        assertEquals(0, BigDecimal.valueOf(50.00).compareTo(
                userWallet.getTotalBalance()));
    }

    @Test
    @DisplayName("Test whether trying to debit an amount larger than the" +
            " available balance returns false.")
    public void tryDeductFromTotalBalanceFalse() {
        boolean result = userWallet.tryDeductFromTotalBalance(
                BigDecimal.valueOf(150.00));
        assertFalse(result);
        assertEquals(0, BigDecimal.valueOf(100.00).compareTo(
                userWallet.getTotalBalance()));
    }

    @Test
    @DisplayName("Test whether trying to deduct no. of shares smaller than the" +
            " available shares returns true.")
    public void tryDeductFromAvailableSharesTrue() {
        boolean result = userWallet.tryDeductFromAvailableShares(50);
        assertTrue(result);
        assertEquals(50, userWallet.getAvailableShares());
    }

    @Test
    @DisplayName("Test whether trying to debit an amount larger than the" +
            " available balance returns false.")
    public void tryDeductFromAvailableSharesFalse() {
        boolean result = userWallet.tryDeductFromAvailableShares(150);
        assertFalse(result);
        assertEquals(100, userWallet.getAvailableShares());
    }

    @Test
    @DisplayName("Test whether trying to deduct no. of shares smaller than the" +
            " available shares returns true.")
    public void tryDeductFromTotalSharesTrue() {
        boolean result = userWallet.tryDeductFromTotalShares(50);
        assertTrue(result);
        assertEquals(50, userWallet.getTotalShares());
    }

    @Test
    @DisplayName("Test whether trying to debit an amount larger than the" +
            " available balance returns false.")
    public void tryDeductFromTotalSharesFalse() {
        boolean result = userWallet.tryDeductFromTotalShares(150);
        assertFalse(result);
        assertEquals(100, userWallet.getTotalShares());
    }
}
