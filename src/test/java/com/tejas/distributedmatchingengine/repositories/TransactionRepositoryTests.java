package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Transaction;
import com.tejas.distributedmatchingengine.models.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRepositoryTests {
    private ConcurrentNavigableMap<Long, Transaction> transactions;
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactions = new ConcurrentSkipListMap<>();
        transactionRepository = new TransactionRepository(transactions);
    }

    @Test
    @DisplayName("Test that a transaction is added to the ledger.")
    void addTransaction() {
        Order sellOrder = new Order("user123",
                Type.SELL, BigDecimal.valueOf(12.00), 20);

        Order buyOrder = new Order("user124",
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        assertTrue(transactions.isEmpty());

        transactionRepository.addTransaction(1L,
                Instant.now(),
                sellOrder.getOrderId(),
                buyOrder.getOrderId(),
                BigDecimal.valueOf(20),
                20);

        assertFalse(transactions.isEmpty());
    }
}