package com.tejas.distributedmatchingengine.services;

import com.tejas.distributedmatchingengine.models.Order;
import com.tejas.distributedmatchingengine.models.Type;
import com.tejas.distributedmatchingengine.repositories.OrderRepository;
import com.tejas.distributedmatchingengine.repositories.TransactionRepository;
import com.tejas.distributedmatchingengine.repositories.UserRepository;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

// SampleTime gets the p50, p95, etc. latencies
// 2 modes => 1 fork for each benchmark mode
@BenchmarkMode({Mode.SampleTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public class MatchingEngineBenchmark {

    private MatchingService matchingService;
    private ExecutorService settlementHandler;

    private String sellerUserId;
    private String buyerUserId;

    private long prevTransactionCount = 0;
    private long invocations;

    // Fixed prices and quantities to avoid creation of BigDecimal objects in hot loop
    private static final BigDecimal PRICE_100 = BigDecimal.valueOf(100.00);
    private static final BigDecimal PRICE_102 = BigDecimal.valueOf(102.00);
    private static final BigDecimal PRICE_110 = BigDecimal.valueOf(110.00);

    @Setup(Level.Trial)
    public void setupTrial() {
        // Turn off logging to prevent pollution of the benchmark scores
        Logger.getLogger("com.tejas.distributedmatchingengine")
                .setLevel(java.util.logging.Level.WARNING);
    }

    @Setup(Level.Iteration)
    public void setup() {
        // Use daemon threads to allow the JVM to exit when settlement
        // is still being executed (since only the matching thread is
        // being benchmarked).
        settlementHandler = Executors.newFixedThreadPool(8);

        UserRepository userRepository = new UserRepository();
        TransactionRepository transactionRepository = new TransactionRepository();
        SettlementService settlementService = new SettlementService(userRepository, settlementHandler);

        // Create new order books in each iteration so that orders in the
        // next iteration don't match orders with the orders in the old books.
        OrderRepository orderRepository = new OrderRepository();
        matchingService = new MatchingService(orderRepository,
                settlementService, transactionRepository);

        // Pre-create buyer and seller once with large balances
        // This avoids creating a lot of UserWallet objects during the iteration
        // reducing GC pressure
        sellerUserId = userRepository.addUserWithShares(BigDecimal.valueOf(0.0), Integer.MAX_VALUE);
        buyerUserId = userRepository.addUser(BigDecimal.valueOf(1e12));
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() {
        long currTransactionCount = MatchingService.getNextTransactionID().longValue() - prevTransactionCount;
        // Sanity check that the number of successful matches equal the invocation count
        System.out.printf("Successful Matches = %d Invocations = %d%n",
                (currTransactionCount - 1) / 2, invocations);
        invocations = 0L;
        prevTransactionCount += currTransactionCount;

        // Prevent leaking thread pools across iterations since live threads
        // of a pool are GC roots
        if (settlementHandler != null) {
            settlementHandler.shutdownNow();
        }
    }

    @Benchmark
    public void benchmarkAggressiveBuySweep(Blackhole bh) {
        invocations++;

        Order askLevel1 = new Order(sellerUserId, Type.SELL, PRICE_100, 10);
        Order askLevel2 = new Order(sellerUserId, Type.SELL, PRICE_102, 15);
        matchingService.matchSell(askLevel1);
        matchingService.matchSell(askLevel2);

        Order aggressiveBuy = new Order(buyerUserId, Type.BUY, PRICE_110, 25);

        bh.consume(matchingService.matchBuy(aggressiveBuy));
    }

    static void main() throws Exception {
        Options opt = new OptionsBuilder()
                .include(MatchingEngineBenchmark.class.getName())
                .shouldFailOnError(true)
                .build();

        new Runner(opt).run();
    }
}