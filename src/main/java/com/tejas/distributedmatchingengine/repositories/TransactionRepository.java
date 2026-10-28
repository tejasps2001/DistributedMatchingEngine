package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Transaction;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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
        // Using skip list map for sorting the transactions based on
        // the order execution time
        this.transactions = new ConcurrentSkipListMap<>();
    }

    // Package-private constructor for testing purposes
    TransactionRepository(ConcurrentMap<Instant, Transaction> transactions) {
        this.transactions = transactions;
    }

    public void addTransaction(Order restingOrder,
                               Order incomingOrder,
                               int qtyFulfilled) {
        Transaction newTransaction = new Transaction(restingOrder,
                incomingOrder, qtyFulfilled);
        transactions.put(Instant.now(), newTransaction);
        logger.info(newTransaction.toString());
    }
}
