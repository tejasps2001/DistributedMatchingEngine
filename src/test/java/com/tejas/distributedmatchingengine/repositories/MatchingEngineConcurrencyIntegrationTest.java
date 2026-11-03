// Residing in repositories packages to benefit from the package-private
// OrderRepository constructor
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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

// Not using @SpringBootTest because new objects are needed for every
// repetition. Therefore, we have to create them in setUp() anyway.
// Because of removing @SpringBootTest, the tests will also run faster.
class MatchingEngineConcurrencyIntegrationTest {
    private OrderController orderController;
    private final String[] user = new String[10];
    private final Order[] orders = new Order[500];
    private final UserWallet[] wallets = new UserWallet[10];

    private final RandomGenerator generator = new Random();
    private ExecutorService workers;
    private ExecutorService settlementHandler;
    private BigDecimal initialTotalBalanceSum;
    private int initialTotalSharesSum;
    private int totalQuantity;
    private int finalTotalSharesSum;
    private int filledQuantity;
    private int unfilledQuantity;


    @BeforeEach
    void setUp() {
        totalQuantity = 0;
        finalTotalSharesSum = 0;
        filledQuantity = 0;
        unfilledQuantity = 0;
        // The executors are shutdown after each test. Hence, initiate them
        // before each test run
        workers = Executors.newFixedThreadPool(20);
        settlementHandler = Executors.newFixedThreadPool(8);
        UserRepository userRepository = new UserRepository();
        UserController userController = new UserController(userRepository);
        TransactionRepository transactionRepository = new TransactionRepository();
        SettlementService settlementService =
                new SettlementService(userRepository, settlementHandler);

        // Create new order books in each test run so that new test runs
        // don't match orders with the orders in the old books.
        OrderRepository orderRepository = new OrderRepository(
                new TreeMap<>(Comparator.reverseOrder()),
                new TreeMap<>(Comparator.naturalOrder())
        );
        MatchingService matchingService =
                new MatchingService(orderRepository,
                        settlementService, transactionRepository);

        // Need to pass all the above to a fresh orderController instead of
        // using autowiring lest the order book already present in the context
        // will be used (previous repetition's order books) which can have
        // unfilled orders.
        orderController =
                new OrderController(matchingService, userRepository);

        // Large balances and shares so there are no insufficient asset errors
        for (int i = 0; i < 10; i++) {
            user[i] = userController.createNewUser(
                    10000000.00).getBody();
            wallets[i] = userController.getWallet(user[i]).getBody();
            assert wallets[i] != null;
            wallets[i].addToTotalShares(100000);
            wallets[i].addToAvailableShares(100000);
        }

        initialTotalBalanceSum = new BigDecimal("100000000.00");
        initialTotalSharesSum = 100000 * 10;

        // Restricting half of the users to buyers and the other half to sellers
        // to prevent invoking the self-trade prevention policy.
        int i = 0;
        while (i < 500) {
            orders[i] = new Order(user[generator.nextInt(0, 5)],
                    Type.BUY,
                    BigDecimal.valueOf(generator.nextDouble(
                            9.00, 11.00)),
                    generator.nextInt(10, 21)

            );
            totalQuantity += orders[i].getQuantity();

            i++;
            orders[i] = new Order(user[generator.nextInt(5, 10)],
                    Type.SELL,
                    BigDecimal.valueOf(generator.nextDouble(
                            9.00, 11.00)),
                    generator.nextInt(10, 21)
            );
            totalQuantity += orders[i].getQuantity();
            i++;
        }

        int totalShares = 0;
        for (UserWallet wallet : wallets) {
            totalShares += wallet.getTotalShares();
        }
        assertEquals(initialTotalSharesSum, totalShares);
        assertTrue(orderRepository.getAsksBook().isEmpty());
        assertTrue(orderRepository.getBidsBook().isEmpty());
    }

    @Test
    @DisplayName("Concurrent load: Total balance and shares are conserved, no" +
            " exceptions, no negative wallets.")
    void concurrentOrdersConserveTotalBalanceAndShares() {
        CountDownLatch latch = new CountDownLatch(1);
        var allOrderResults = new HashMap<Order, Future<ResponseEntity<String>>>();
        BigDecimal finalTotalBalanceSum =
                new BigDecimal("0.0");

        // Each thread will execute 25 orders
        int j;
        for (j = 0; j < 500; j++) {
            int i = j;
            var result = workers.submit(() -> {
                ResponseEntity<String> orderResult;
                try {
                    latch.await();
                    if(orders[i].getType() == Type.BUY)
                        orderResult = orderController.placeBuy(orders[i]);
                    else
                        orderResult = orderController.placeSell(orders[i]);
                    return orderResult;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Throwable cause = e.getCause();
                    return ResponseEntity.status(
                                    HttpStatusCode.valueOf(404)).
                            body(cause.getMessage());
                }
            });
            allOrderResults.put(orders[i], result);
        }
        latch.countDown();

        // The workers' shutdown() must be called first because it should
        // call all the necessary settle() calls BEFORE the settlementHandler is
        // shutdown.
        try {
            workers.shutdown();
            if (!workers.awaitTermination(1, TimeUnit.MINUTES)) {
                throw new AssertionError("The workers threads didn't " +
                        "terminate correctly.");
            }
            settlementHandler.shutdown();
            // Wait until the settlement executor is terminated. That indicates
            // that all the orders have had a chance to get executed.
            if (settlementHandler.awaitTermination(
                    1, TimeUnit.MINUTES)) {
                for (Map.Entry<Order, Future<ResponseEntity<String>>> result :
                        allOrderResults.entrySet()) {
                    assertDoesNotThrow(() -> {
                        var response = result.getValue().get();
                        assertTrue(response.getStatusCode().is2xxSuccessful());
                        unfilledQuantity += result.getKey().getRemainingQuantity();
                        filledQuantity +=
                                (result.getKey().getQuantity() -
                                        result.getKey().getRemainingQuantity());
                    });
                }

                for (UserWallet wallet : wallets) {
                    assertTrue(wallet.getAvailableBalance().
                            compareTo(BigDecimal.valueOf(0.0)) >= 0);

                    assertTrue(wallet.getTotalBalance().
                            compareTo(BigDecimal.valueOf(0.0)) >= 0);
                    finalTotalBalanceSum =
                            finalTotalBalanceSum.add(
                                    wallet.getTotalBalance());

                    assertTrue(wallet.getAvailableShares() >= 0);

                    assertTrue(wallet.getTotalShares() >= 0);
                    finalTotalSharesSum += wallet.getTotalShares();

                    // for each wallet, the totalBalance - availableBalance equals the
                    // value locked in that user’s still-resting BUY orders.
                    // Likewise for shares and resting sells.
                    var moneyInEscrow = wallet.getTotalBalance().subtract(
                            wallet.getAvailableBalance());
                    var escrowMoneyInBuyOrders = wallet.getOrderList().stream().
                            filter(order -> order.getStatus() ==
                                    Status.NEW || order.getStatus() ==
                                    Status.PARTIALLY_FILLED).
                            filter(order -> order.getType() == Type.BUY).
                            map(order -> order.getPrice().multiply(
                                    BigDecimal.valueOf(order.getRemainingQuantity()
                                    ))).
                            reduce(BigDecimal.ZERO, BigDecimal::add);
                    assertEquals(0, moneyInEscrow
                            .compareTo(escrowMoneyInBuyOrders));

                    var sharesInEscrow = wallet.getTotalShares() -
                            wallet.getAvailableShares();
                    var escrowSharesInSellOrders = wallet.getOrderList()
                            .stream()
                            .filter(order -> order.getStatus() == Status.NEW ||
                                    order.getStatus() ==
                                            Status.PARTIALLY_FILLED)
                            .filter(order -> order.getType() == Type.SELL)
                            .mapToInt(Order::getRemainingQuantity)
                            .sum();
                    assertEquals(sharesInEscrow, escrowSharesInSellOrders);
                }

                assertEquals(initialTotalSharesSum, finalTotalSharesSum);
                assertEquals(0, initialTotalBalanceSum.compareTo(
                        finalTotalBalanceSum));

                // Total quantity submitted equals filled quantity (counted on both
                // sides) plus quantity still resting
                assertEquals(totalQuantity,
                        filledQuantity + unfilledQuantity);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
