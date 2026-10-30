package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.models.Transaction;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.*;

@Repository
public class TransactionRepository {
    // Using a concurrency supporting data structure here because one user
    // may be executing multiple orders (of different symbol pairs) at the
    // same time.
    // Use timestamp as the key.
    // Using a sorting data structure to sort based on the transactionID.
    @Getter
    private final ConcurrentNavigableMap<Long, Transaction> transactionsLedger;

    public TransactionRepository() {
        // Using skip list map for sorting the transactions based on
        // the order execution time
        this.transactionsLedger = new ConcurrentSkipListMap<>();
    }

    // Package-private constructor for testing purposes
    TransactionRepository(ConcurrentNavigableMap<Long, Transaction> transactionsLedger) {
        this.transactionsLedger = transactionsLedger;
    }

    public void addTransaction(long transactionID,
                               Instant executionTime,
                               long restingOrderID,
                               long incomingOrderID,
                               BigDecimal executionPrice,
                               int qtyFulfilled) {
        Transaction newTransaction = new Transaction(
                transactionID,
                executionTime,
                restingOrderID,
                incomingOrderID,
                executionPrice,
                qtyFulfilled);
        transactionsLedger.put(transactionID, newTransaction);
    }
}
