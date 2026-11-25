# Real-Time Limit Order Matching Engine

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![CI](https://github.com/tejasps2001/DistributedMatchingEngine/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/tejasps2001/DistributedMatchingEngine/actions/workflows/ci.yml)
[![JMH](https://img.shields.io/badge/Benchmarked%20with-JMH-blue.svg)](https://github.com/openjdk/jmh)
[![k6](https://img.shields.io/badge/Load%20Tested%20with-k6-purple.svg)](https://k6.io/)

A deterministic, low-latency price-time priority (FIFO) Limit Order Book (LOB)
and double-entry escrow ledger engineered in Java 25 and Spring Boot 3.

The engine implements **deterministic matching under a single lock** decoupled
from an **asynchronous settlement thread pool**, sustaining **~138,000
matches/sec** in memory with a **5.17 µs median latency**, and sustained **1,000 HTTP req/sec** end-to-end through
Spring MVC with zero errors.

---

## Architecture Overview

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
                  |   Self-Trade Prevention (STP) |
                  └──────────────┬────────────────┘
                                 │
                        (Synchronized Match Path)
                                 ▼
                  ┌───────────────────────────────┐
                  │    Matching Engine (FIFO)     │
                  │  - Price Cross Detection      │
                  │  - Multi-Level Book Walking   │
                  │  - Price-Improvement Refunds  │
                  └──────────────┬────────────────┘
                                 │
                        (Async Thread Pool)
                                 ▼
                  ┌───────────────────────────────┐
                  │             Settlement        │
                  │  - Balance Transfers          │
                  │  - Shares Settlement          │
                  └───────────────────────────────┘

### Core Design Decisions

1. **Deterministic matching under a single lock:** Eliminates read/write lock
   thrashing across hot order books by synchronizing order matching on a
   dedicated matching thread.
2. **Asynchronous Settlement Pipeline:** Separates low-latency matching from
   post-trade balance settlement using a managed worker pool.
3. **Double-Entry Escrow Invariant:** Wallets enforce strict segregation
   between available and escrow balances. Funds for buying and shares for 
   selling are pre-allocated at order placement to prevent double-spending or
   negative balances.
4. **Taker Price Improvement:** Aggressive limit orders sweeping deep resting
   orders execute at maker prices; the remaining surplus between reserved
   escrow and actual trade execution is credited back to the taker's wallet.
5. **Self-Trade Prevention (STP):** Detects wash-trading when matching
   opposing orders from identical user IDs, immediately cancelling and
   refunding the aggressive incoming order to prevent artificial volume
   generation.

---

## Benchmarks

The engine was evaluated at two layers: an **in-memory microbenchmark (JMH)**
isolating the pure execution core, and a **HTTP-level load test (k6)**
measuring the complete Spring MVC REST pipeline.

### 1. In-Memory Matching Core (JMH)

Micro benchmarks were run on OpenJDK 25 (LTS) with a compiler black hole
consumer:

#### Latency Percentiles

| Percentile       | Latency       | 
|:-----------------|:--------------|
| **p50 (Median)** | **5.17 µs**   |
| **p90**          | **6.85 µs**   |
| **p95**          | **8.5 µs**    |
| **p99**          | **30.209 µs** |
| **p100 (Max)**   | **31.23 ms**  |

<small>The full JMH benchmark result is [here](benchmarks/jmh/results-24-11-2026.txt).</small>
---

### 2. End-to-End Load Test (k6)

Macro load testing executed a constant arrival rate 
of **1,000 HTTP orders/sec** across a maximum of 500 Virtual Users
(VUs) over a 30-second measurement window:

```
HTTP Req Duration: p(95) = 1.05ms, p(99) = 1.64ms
HTTP Req Failure Rate: 0.00% (0 errors out of 30,001 total requests)
Checks Succeeded: 100.00%
```

| Metric                    | Result            | Target SLA    | Status                     |
|:--------------------------|:------------------|:--------------|:---------------------------|
| **Throughput**            | **999.7 req/sec** | 1,000 req/sec | **Passed**                 |
| **Median Duration (p50)** | **814.9 µs**      | < 10.0 ms     | **Passed (< 1 ms)**        |
| **p95 Duration**          | **1.05 ms**       | < 50.0 ms     | **Passed**                 |
| **p99 Duration**          | **1.64 ms**       | < 150.0 ms    | **Passed**                 |
| **Error Rate**            | **0.00%**         | < 1.00%       | **Passed (Zero Failures)** |

<small>The full k6 benchmark result is [here](benchmarks/k6/results-1000rps.txt).</small>
---

### 3. Benchmark metadata

| Hardware type | Model                                  |
|:--------------|:---------------------------------------|
| **CPU**       | AMD Ryzen 5 5500U                      |
| **RAM**       | 8GiB SODIMM DDR4 x2                    |
| **OS**        | Ubuntu 26.04.1 LTS                     |
| **JVM**       | OpenJDK 64-Bit Server VM, 25.0.3+9-LTS |
| **Commit**    | 46172c1                                |

---

### Interpretation of the benchmarks
* HTTP and JSON overhead dominate the CPU for the entire process.
* The actual order matching code takes up very little of the CPU time.

### Prerequisites

* **Java 25** or later
* **Maven 3.9+** (or the included `./mvnw` wrapper)
* **k6** (optional, for load testing)

### Build and Run Tests

```bash
# Run unit and integration tests
./mvnw clean test-compile
```

### Run the Application

```bash
./mvnw spring-boot:run
```

The application runs on `http://localhost:8080`.

## API Specification

The REST boundary validates incoming payloads via Jakarta Bean Validation
before dispatching to the order book. 
**First, a user with a wallet consisting of a balance (and shares, optionally)
must be created.**

### 1. User & Wallet Management

* `POST /createUser?balance=10000` Initializes a new user with the specified
  balance and returns the unique `userid`.
* `POST /createUserWithShares?balance=10000.00&shares=500`
  Initializes a new user with the specified balance and shares and returns
  the unique `userid`.
* `GET /wallet?userid={userid}`
  Inspects real-time `Available` vs. `Escrowed` cash and share balances.
* `DELETE /cancel?orderid={orderid}`
  Cancels a resting order and restores encumbered escrow funds.

---

### 2. Place an Order

* **Buy Endpoint:** `POST /buy`
* **Sell Endpoint:** `POST /sell`
* **Headers:** `Content-Type: application/json`

**Example**

```json
{
  "userid": "user_4a7b1c",
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
Swagger/OpenAPI documentation is available at `http://localhost:8080/swagger-ui.html`.
