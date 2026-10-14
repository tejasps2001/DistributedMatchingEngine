package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.models.Transaction;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

@Repository
public class TransactionRepository {
    // Using a concurrency supporting data structure here because one user
    // may be executing multiple orders (of different symbol pairs) at the
    // same time.
    // Use timestamp as the key.
    private final ConcurrentMap<Instant, Transaction> transactions;
    private static final Logger logger =
            Logger.getLogger(TransactionRepository.class.getName());
    public TransactionRepository() {
        this.transactions = new ConcurrentSkipListMap<>();
    }

    public void addTransaction(Transaction newTransaction) {
        transactions.put(Instant.now(), newTransaction);
        logger.info(newTransaction.toString());
    }
}
