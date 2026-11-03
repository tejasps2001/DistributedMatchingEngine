package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.controllers.OrderController;
import com.tejas.distributedmatchingengine.controllers.UserController;
import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.Type;
import com.tejas.distributedmatchingengine.models.UserWallet;
import com.tejas.distributedmatchingengine.services.MatchingService;
import com.tejas.distributedmatchingengine.services.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchAndSettleIntegrationTest {
    private OrderController orderController;
    private OrderRepository orderRepository;
    private UserRepository userRepository;
    private ExecutorService settlementHandler;
    private TransactionRepository transactionRepository;

    private String newSellingUser;
    private String newBuyingUser;

    private UserWallet buyerWallet;
    private UserWallet sellerWallet;

    // Setup: create buyer and seller with appropriate balances and shares
    @BeforeEach
    void setUp() {
        settlementHandler = Executors.newFixedThreadPool(8);
        userRepository = new UserRepository();
        UserController userController = new UserController(userRepository);
        transactionRepository = new TransactionRepository();
        SettlementService settlementService = new SettlementService(userRepository, settlementHandler);

        // Create new order books in each test run so that new test runs
        // don't match orders with the orders in the old books.
        orderRepository = new OrderRepository(
                new TreeMap<>(Comparator.reverseOrder()),
                new TreeMap<>(Comparator.naturalOrder())
        );
        MatchingService matchingService = new MatchingService(orderRepository,
                settlementService, transactionRepository);

        // Need to pass all the above to a fresh orderController instead of
        // using autowiring lest the order book already present in the context
        // will be used (previous repetition's order books) which can have
        // unfilled orders.
        orderController =
                new OrderController(matchingService,
                        userRepository, orderRepository);

        newBuyingUser = userController.createNewUser(240.00).getBody();
        newSellingUser = userController.createNewUser(0).getBody();
        buyerWallet = userController.getWallet(newBuyingUser).getBody();
        sellerWallet = userController.getWallet(newSellingUser).getBody();
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
        // as soon as it arrives. Hence, the bids book must be empty.
        assertTrue(orderRepository.getBidsBook().isEmpty());

        // Settlement happens via an async executor. Therefore, we have to wait
        // until the wallets arrive to their final state. Instead of adding a
        // timeout for each assertion, use Awaitility to wait on the whole block
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
        // as soon as it arrives. Hence, the asks book must be empty.
        assertTrue(orderRepository.getAsksBook().isEmpty());

        // Settlement happens via an async executor. Therefore, we have to wait
        // until the wallets arrive to their final state. Instead of adding a
        // timeout for each assertion, use Awaitility to wait on the whole block
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

    @Test
    @DisplayName("Cancelling a resting limit buy order must restore escrow back to available balance")
    void cancelOrderShouldReleaseEscrowBackToAvailableBalance() {
        String buyingUserId = userRepository.addUser(BigDecimal.valueOf(1000.00));
        String sellingUserId = userRepository.addUserWithShares(BigDecimal.valueOf(1000.00), 10);
        // Order to be canceled
        Order buyOrder = new Order(buyingUserId, Type.BUY, new BigDecimal("100.00"), 5);
        // Order that triggers the removal of the above canceled BUY order
        Order sellOrder = new Order(sellingUserId, Type.SELL, new BigDecimal("100.00"), 5);

        orderController.placeBuy(buyOrder);

        UserWallet wallet = userRepository.findUserByID(buyingUserId).orElseThrow();
        assertEquals(0, wallet.getAvailableBalance().compareTo(BigDecimal.valueOf(500.00)));
        assertEquals(0, wallet.getTotalBalance().compareTo(BigDecimal.valueOf(1000.00)));
        // Verify that the buyOrder is in the bidsBook
        assertTrue(orderRepository.getBidsBook().get(buyOrder.getPrice()).contains(buyOrder));

        var cancelStatus =  orderController.cancelOrder(buyOrder.getOrderId());
        assertEquals("Order cancelled.", cancelStatus.getBody());
        assertEquals(Status.CANCELLED, buyOrder.getStatus());

        // Verify the canceled order leads to restoration of the wallet balance
        orderController.placeSell(sellOrder);
        settlementHandler.shutdown();
        try {
            settlementHandler.awaitTermination(30, TimeUnit.SECONDS);
            assertEquals(0, wallet.getAvailableBalance()
                    .compareTo(BigDecimal.valueOf(1000.00)));
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

    }
}
