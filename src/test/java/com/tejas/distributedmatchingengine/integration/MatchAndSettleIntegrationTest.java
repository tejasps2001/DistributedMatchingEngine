package com.tejas.distributedmatchingengine.integration;

import com.tejas.distributedmatchingengine.controllers.OrderController;
import com.tejas.distributedmatchingengine.controllers.UserController;
import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.Type;
import com.tejas.distributedmatchingengine.models.UserWallet;
import com.tejas.distributedmatchingengine.repositories.OrderRepository;
import com.tejas.distributedmatchingengine.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class MatchAndSettleIntegrationTest {
    @Autowired
    private OrderController orderController;

    @Autowired
    private UserController userController;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private String newSellingUser;
    private String newBuyingUser;

    private UserWallet buyerWallet;
    private UserWallet sellerWallet;

    // Setup: create buyer and seller with appropriate balances and shares
    @BeforeEach
    void setUp() {
        newBuyingUser = userController.createNewUser(240.00).getBody();
        newSellingUser = userController.createNewUser(0).getBody();
        buyerWallet = userController.getPortfolio(newBuyingUser).getBody();
        sellerWallet = userController.getPortfolio(newSellingUser).getBody();
        assert buyerWallet != null;
        assert sellerWallet != null;
        sellerWallet.addToTotalShares(20);
        sellerWallet.addToAvailableShares(20);
    }

    @Test
    void matchAndSettleWithBuyerPriceImprovement() {
        // Resting sell
        Order sellOrder = new Order(newSellingUser,
                Type.SELL, BigDecimal.valueOf(10.00), 20);
        // Incoming aggressive buy
        Order buyOrder = new Order(newBuyingUser,
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        var orderStatus = orderController.placeSell(sellOrder);
        assertEquals("Couldn't fill order. Added to order book.",
                orderStatus.getBody());
        assertEquals(Status.NEW, sellOrder.getStatus());

        orderStatus = orderController.placeBuy(buyOrder);
        assertEquals("Order executed.", orderStatus.getBody());
        assertEquals(Status.FILLED, buyOrder.getStatus());
        assertEquals(Status.FILLED, sellOrder.getStatus());

        assertTrue(orderRepository.getAsksBook()
                .firstEntry().getValue().isEmpty());
        // The buy order isn't added to the bids book because it gets matched
        // as soon as it arrives
        assertTrue(orderRepository.getBidsBook().isEmpty());

        // Settlement happens via an async executor. Therefore, have to wait
        // until the wallets arrive to their final state. Instead of adding a
        // timeout for each assertion, use Awaitlity to wait on the whole block
        // of assertions.
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            var transactionLedger = transactionRepository.getTransactionsLedger();
            var transaction = transactionLedger.firstEntry().getValue();
            assertEquals(BigDecimal.valueOf(10.00), transaction.executionPrice());

            assertEquals(20, buyerWallet.getTotalShares());
            assertEquals(20, buyerWallet.getAvailableShares());
            assertEquals(BigDecimal.valueOf(40.00), buyerWallet.getTotalBalance());
            assertEquals(BigDecimal.valueOf(40.00), buyerWallet.getAvailableBalance());

            assertEquals(0, sellerWallet.getTotalShares());
            assertEquals(0, sellerWallet.getAvailableShares());
            assertEquals(BigDecimal.valueOf(200.00), sellerWallet.getTotalBalance());
            assertEquals(BigDecimal.valueOf(200.00), sellerWallet.getAvailableBalance());
        });
    }

    @Test
    void matchAndSettleWithSellerPriceImprovement() {
        // Incoming aggressive sell
        Order sellOrder = new Order(newSellingUser,
                Type.SELL, BigDecimal.valueOf(10.00), 20);
        // Resting buy
        Order buyOrder = new Order(newBuyingUser,
                Type.BUY, BigDecimal.valueOf(12.00), 20);

        var orderStatus = orderController.placeBuy(buyOrder);
        assertEquals("Couldn't fill order. Added to order book.",
                orderStatus.getBody());
        assertEquals(Status.NEW, buyOrder.getStatus());

        orderStatus = orderController.placeSell(sellOrder);
        assertEquals("Order executed.", orderStatus.getBody());
        assertEquals(Status.FILLED, buyOrder.getStatus());
        assertEquals(Status.FILLED, sellOrder.getStatus());

        assertTrue(orderRepository.getBidsBook()
                .firstEntry().getValue().isEmpty());
        // The sell order isn't added to the asks book because it gets matched
        // as soon as it arrives
        assertTrue(orderRepository.getAsksBook().isEmpty());

        // Settlement happens via an async executor. Therefore, have to wait
        // until the wallets arrive to their final state. Instead of adding a
        // timeout for each assertion, use Awaitlity to wait on the whole block
        // of assertions.
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            var transactionLedger = transactionRepository.getTransactionsLedger();
            var transaction = transactionLedger.firstEntry().getValue();
            assertEquals(BigDecimal.valueOf(12.00), transaction.executionPrice());

            assertEquals(20, buyerWallet.getTotalShares());
            assertEquals(20, buyerWallet.getAvailableShares());
            assertEquals(BigDecimal.valueOf(0.00), buyerWallet.getTotalBalance());
            assertEquals(BigDecimal.valueOf(0.00), buyerWallet.getAvailableBalance());

            assertEquals(0, sellerWallet.getTotalShares());
            assertEquals(0, sellerWallet.getAvailableShares());
            assertEquals(BigDecimal.valueOf(240.00), sellerWallet.getTotalBalance());
            assertEquals(BigDecimal.valueOf(240.00), sellerWallet.getAvailableBalance());
        });
    }
}
