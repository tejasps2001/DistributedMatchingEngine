# Real-Time Limit Order Matching Engine

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![JMH](https://img.shields.io/badge/Benchmarked%20with-JMH-blue.svg)](https://github.com/openjdk/jmh)
[![k6](https://img.shields.io/badge/Load%20Tested%20with-k6-purple.svg)](https://k6.io/)

A deterministic, low-latency price-time priority (FIFO) Limit Order Book (LOB) and
double-entry escrow ledger engineered in Java 21 and Spring Boot 3.

The engine implements a **single-writer matching core** decoupled from an
**asynchronous settlement thread pool**, sustaining **~100,000 matches/sec**
in memory with a **5.61 µs median latency**, and **1,000 HTTP req/sec** end-to-end 
through Spring MVC with zero errors.

---

## Architecture Overview


```

```
                      HTTP REST Client (k6 / Web)
                                  │
                                  ▼
                  ┌───────────────────────────────┐
                  │    Spring Boot 3 Web Layer    │
                  │   Jakarta Bean Validation     │
                  └──────────────┬────────────────┘
                                 │
                                 ▼
                  ┌───────────────────────────────┐
                  │    Pre-Trade Balance Check    │
                  │   (Encumber Available Funds)  │
                  └──────────────┬────────────────┘
                                 │
                    (Single-Writer Synchronized)
                                 ▼
                  ┌───────────────────────────────┐
                  │    Matching Engine (FIFO)     │
                  │  - Price Cross Detection      │
                  │  - Multi-Level Book Walking   │
                  │  - Self-Trade Prevention (STP)│
                  │  - Price-Improvement Refunds  │
                  └──────────────┬────────────────┘
                                 │
                    (Async Daemon Thread Pool)
                                 ▼
                  ┌───────────────────────────────┐
                  │   Settlement & Ledger Core    │
                  │  - Atomic Balance Transfers   │
                  │  - Serial Transaction Ledger  │
                  └───────────────────────────────┘

```

```

### Core Design Decisions

1. **Deterministic Single-Writer Core:** Eliminates read/write lock thrashing
across hot order books by synchronizing order matching on a dedicated execution
sequence.
2. **Asynchronous Settlement Pipeline:** Separates low-latency matching from
post-trade balance settlement and trade ledger auditing using a managed daemon
worker pool.
3. **Double-Entry Escrow Invariant:** Wallets enforce strict segregation between
available and escrow balances. Buy funds and sell shares are pre-allocated at
order placement to prevent double-spending or negative balances.
4. **Taker Price Improvement:** Aggressive limit orders sweeping deep resting
orders execute at maker prices; the remaining surplus between reserved escrow 
and actual trade execution is atomically credited back to the taker's wallet.
5. **Self-Trade Prevention (STP):** Detects wash-trading patterns when matching
opposing orders from identical user IDs, immediately cancelling and refunding
the aggressive incoming order to prevent artificial volume generation.

---

## Verified Benchmarks

The engine was evaluated at two layers: an **in-memory microbenchmark (JMH)**
isolating the pure execution core, and a **macro-level load test (k6)** measuring
the complete Spring MVC REST pipeline.

### 1. In-Memory Matching Core (JMH)

Microbenchmarks were run on OpenJDK 25 (LTS) using continuous multi-level
liquidity sweeps across multiple price tiers with compiler black hole consumer
isolation:


```

Benchmark                                  Mode       Count      Score        Error     Units
MatchingEngineBenchmark.aggressiveBuySweep thrpt          5      0.094  ±     0.021    ops/µs (~94k - 101k ops/sec)
MatchingEngineBenchmark.aggressiveBuySweep sample    401,171      9.777  ±     0.563     µs/op

```

#### Latency Percentiles (Pure In-Memory Matching)

| Percentile       | Latency       | Evaluation Context                                          |
|:-----------------|:--------------|:------------------------------------------------------------|
| **p50 (Median)** | **5.61 µs**   | Half of all multi-level matches clear in ~5.6 microseconds. |
| **p90**          | **8.06 µs**   | 90% of order sweeps complete under 9 microseconds.          |
| **p95**          | **13.57 µs**  | High-percentile boundary under continuous load.             |
| **p99**          | **103.04 µs** | Tail distribution including task queuing.                   |
| **p100 (Max)**   | **21.92 ms**  | Lowest recorded STW young-generation GC safepoint pause.    |

---

### 2. Full-Stack End-to-End Load Test (k6)

Macro load testing executed a sustained arrival rate of **1,000 HTTP orders/sec**
across 50 concurrent Tomcat workers over a 30-second measurement window:


```

HTTP Req Duration: p(95) = 2.58ms, p(99) = 22.54ms
HTTP Req Failure Rate: 0.00% (0 errors out of 29,826 total requests)
Checks Succeeded: 100.00% (29,806 out of 29,806 valid status assertions)

```

| Metric                    | Result            | Target SLA    | Status                     |
|:--------------------------|:------------------|:--------------|:---------------------------|
| **Throughput**            | **987.6 req/sec** | 1,000 req/sec | **Passed**                 |
| **Median Duration (p50)** | **988.91 µs**     | < 10.0 ms     | **Passed (< 1 ms)**        |
| **p95 Duration**          | **2.58 ms**       | < 50.0 ms     | **Passed**                 |
| **p99 Duration**          | **22.54 ms**      | < 150.0 ms    | **Passed**                 |
| **Error Rate**            | **0.00%**         | < 1.00%       | **Passed (Zero Failures)** |

---

## API Specification

The REST boundary validates incoming payloads via Jakarta Bean Validation
before dispatching to the order book.

First, a user with a wallet consisting of a balance (and shares, optionally)
must be created.

### 1. User & Wallet Management

* `POST /createUser?balance=10000` Initializes a new user with the specified 
 balance and returns the unique `userId`.
* `POST /createUserWithShares?balance=10000.00&shares=500`
  Initializes a new user with the specified balance and shares and returns
the unique `userId`.
* `GET /wallet?userID={userId}`
  Inspects real-time `Available` vs. `Escrowed` cash and share balances.
* `DELETE /cancel?orderID={orderId}`
  Cancels a resting order and restores encumbered escrow funds.

---

### 2. Place an Order

* **Buy Endpoint:** `POST /buy`
* **Sell Endpoint:** `POST /sell`
* **Headers:** `Content-Type: application/json`

```json
{
  "userId": "user_4a7b1c",
  "type": "BUY",
  "price": 105.00,
  "quantity": 10
}

```

#### HTTP Response Codes

* `200 OK`: Order fully filled or partially filled.
* `202 ACCEPTED`: Order could not be immediately matched; added to the Limit Order
Book as a resting order.
* `409 CONFLICT`: Order canceled due to Self-Trade Prevention (STP).
* `422 UNPROCESSABLE ENTITY`: Insufficient available wallet balance or shares.
* `400 BAD REQUEST`: Invalid enum format or missing mandatory fields.

---

## Project Roadmap

✅ **Milestone 1: In-Memory Core & Benchmarks (Completed)**
* Deterministic FIFO limit order book with multi-level book sweeping.

* Multi-asset double-entry wallet escrow ledger with price-improvement refunds.

* Self-trade prevention (STP) guard rails.
* JMH microbenchmarking suite and k6 load verification.


* [ ] **Milestone 2: Relational Persistence & ACID Guarantees (In Progress)**
* PostgreSQL persistence for historical ledgers and trade settlement.

* Pessimistic locking (`SELECT ... FOR UPDATE`) to prevent concurrent wallet
double-spends.

* Flyway automated database migration pipelines.

* Integration testing suite backed by Testcontainers.




* [ ] **Milestone 3: Real-Time Broadcasts & Distributed Clustering**
* Spring WebSocket (STOMP) real-time ticker feeds.

* Redis Pub/Sub for cross-node trade execution broadcasts.

* Redisson distributed locks for order isolation across horizontal instances.





---

## Getting Started

### Prerequisites

* **Java 21** or later
* **Maven 3.9+** (or the included `./mvnw` wrapper)
* **k6** (optional, for load testing)

### Build and Run Tests

```bash
# Run unit and integration tests
./mvnw clean test

# Run JMH Microbenchmarks exclusively
./mvnw test -Dtest=MatchingEngineBenchmark

```

### Run the Application

```bash
./mvnw spring-boot:run

```

The application runs on `http://localhost:8080`. Swagger/OpenAPI documentation
is available at `http://localhost:8080/swagger-ui.html`.
