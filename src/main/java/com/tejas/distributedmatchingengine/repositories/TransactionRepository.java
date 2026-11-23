package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.models.Transaction;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.util.concurrent.*;

@Repository
public class TransactionRepository {
    // Use a concurrency supporting data structure here because multiple
    // matching threads may be recording transactions (of different symbol pairs)
    // at the same time.
    // Use a sorting data structure to sort based on the transactionID.
    @Getter
    private final ConcurrentNavigableMap<Long, Transaction> transactionsLedger;

    public TransactionRepository() {
        this.transactionsLedger = new ConcurrentSkipListMap<>();
    }

    // Package-private constructor for testing purposes
    TransactionRepository(ConcurrentNavigableMap<Long, Transaction> transactionsLedger) {
        this.transactionsLedger = transactionsLedger;
    }

    public void addTransaction(Transaction newTransaction) {
        transactionsLedger.put(newTransaction.transactionID(), newTransaction);
    }
}
