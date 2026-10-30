# Deterministic Limit Order Matching Engine

A high-throughput, deterministic Limit Order Book (LOB) and matching engine built with **Java 21** and **Spring Boot**. The engine executes trades using **Price-Time Priority (FIFO)**, enforces pre-trade financial encumbrance via an in-memory multi-asset wallet ledger, and processes maker-price improvements with surplus lock refunds.

---

## Project Overview & Roadmap Status

*   **Milestone 1 — Core In-Memory Engine (Completed)**
    *   Deterministic FIFO Price-Time Priority LOB implementation.
    *   Multi-asset wallet ledger with strict `Available` vs. `Total` balance encumbrance.
    *   Maker-price trade clearance with automatic taker surplus lock refunds.
    *   Decoupled domain design (`MatchingService` logic isolated from `SettlementService` mutations).
    *   Exhaustive JUnit 5 test suite covering cross-edges, partial fills, and multi-level sweeps.
*   **Milestone 2 — Transactional Persistence & DB Ledger (In Progress)**
    *   PostgreSQL double-entry accounting ledger with pessimistic locking (`SELECT ... FOR UPDATE`).
    *   Flyway schema migrations & integration testing via **Testcontainers**.
*   **Milestone 3 — Real-Time Broadcast & Outbox Pattern (Planned)**
    *   Spring WebSocket (STOMP) integration for live ticker and execution report broadcasts.
    *   Transactional Outbox Pattern for dual-write safety.
*   **Milestone 4 — Distributed Clustering & Locks (Planned)**
    *   Redisson distributed locks and Redis Pub/Sub for multi-node horizontal scaling.

---

## Architectural Design

```text
  [ Incoming Order Request ]
              │
              ▼
  ┌────────────────────────┐
  │  Ingestion Queue /     │  <-- Single-writer design according to the LMAX Disruptor paper
  │  Command Pipeline      │
  └───────────┬────────────┘
              │
              ▼
  ┌────────────────────────┐
  │  Single Matching Core  │  <-- Exclusive ownership of OrderBook state (TreeMap and ArrayDeque)
  │    (MatchingService)   │  <-- Lock-free matching loop execution
  └───────────┬────────────┘
              │
              ├───────────────────────────────────┐
              ▼                                   ▼
  ┌────────────────────────┐          ┌───────────────────────┐
  │ Trade Event Generated  │          │ Order Book State      │
  │  (Maker Price Fixed)   │          │ (Resting Orders Saved)│
  └───────────┬────────────┘          └───────────────────────┘
              │
              ▼
  ┌────────────────────────┐
  │   Settlement Service   │  <-- Settles using the UserWallet (Asset Transfer + Surplus Refund)
  │  (SettlementService)   │
  └────────────────────────┘
