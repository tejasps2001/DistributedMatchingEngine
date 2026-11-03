package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.Type;
import com.tejas.distributedmatchingengine.services.MatchingService;
import com.tejas.distributedmatchingengine.services.SettlementService;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@BenchmarkMode({Mode.SampleTime, Mode.Throughput}) // SampleTime records percentile distributions
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS) // Warm up C2 JIT compiler
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public class MatchingEngineBenchmark {

    private MatchingService matchingService;
    private ExecutorService settlementHandler;

    private String sellerUserId;
    private String buyerUserId;

    // Fixed prices and quantities to avoid BigDecimal instantiation in hot loop
    private static final BigDecimal PRICE_100 = BigDecimal.valueOf(100.00);
    private static final BigDecimal PRICE_102 = BigDecimal.valueOf(102.00);
    private static final BigDecimal PRICE_105 = BigDecimal.valueOf(105.00);
    private static final BigDecimal PRICE_110 = BigDecimal.valueOf(110.00);

    @Setup(Level.Trial)
    public void setupTrial() {
        // Mute JUL loggers to prevent console bottlenecks during benchmarking
        Logger.getLogger("com.tejas.distributedmatchingengine")
                .setLevel(java.util.logging.Level.SEVERE);
    }

    @Setup(Level.Iteration)
    public void setup() {
        // Custom ThreadFactory marking workers as DAEMON
        settlementHandler = Executors.newFixedThreadPool(8, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("settlement-worker");
            return thread;
        });
        UserRepository userRepository = new UserRepository();
        TransactionRepository transactionRepository = new TransactionRepository();
        SettlementService settlementService = new SettlementService(userRepository, settlementHandler);

        // Create new order books in each test run so that new test runs
        // don't match orders with the orders in the old books.
        OrderRepository orderRepository = new OrderRepository(
                new TreeMap<>(Comparator.reverseOrder()),
                new TreeMap<>(Comparator.naturalOrder())
        );
        matchingService = new MatchingService(orderRepository,
                settlementService, transactionRepository);

        // Pre-create buyer and seller once with large balances
        // Avoids creating 500,000 User objects during the iteration
        sellerUserId = userRepository.addUserWithShares(BigDecimal.valueOf(10_000_000.00), 10_000_000);
        buyerUserId = userRepository.addUser(BigDecimal.valueOf(10_000_000.00));
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() {
        // Prevent leaking thread pools across iterations
        if (settlementHandler != null) {
            settlementHandler.shutdownNow();
        }
    }

    @Benchmark
    public void benchmarkAggressiveBuySweep(Blackhole bh) {
        // 1. Replenish two price levels of asks (10 shares @ 100, 15 shares @ 102) = 25 shares total
        Order askLevel1 = new Order(sellerUserId, Type.SELL, PRICE_100, 10);
        Order askLevel2 = new Order(sellerUserId, Type.SELL, PRICE_102, 15);
        matchingService.matchSell(askLevel1);
        matchingService.matchSell(askLevel2);

        // 2. Aggressive taker buy sweeps both levels completely (10 @ 100, 15 @ 102)
        Order aggressiveBuy = new Order(buyerUserId, Type.BUY, PRICE_110, 25);

        bh.consume(matchingService.matchBuy(aggressiveBuy));
    }

    @Test
    void runBenchmarks() throws Exception {
        Options opt = new OptionsBuilder()
                .include(this.getClass().getName())
                .shouldFailOnError(true)
                .build();

        new Runner(opt).run();
    }
}