# Select the execution model for the matching core (Retroactive)

- Date: 2026-11-25

## Context and Problem Statement

Orders come from the multithreaded HTTP controllers and across different
controller actions. Letting these orders be matched in their own threads
concurrently can cause inconsistent order books and race conditions.

## Considered options

- Option A - Use a single dedicated writer thread fed by a ring buffer (the
                LMAX Disruptor paper)
- Option B - Use a lock-free data structure and use Compare-And-Swap
- Option C - Use `synchronized` to serialize matching across threads

## Decision Outcome

Chosen option: "Option C - Use `synchronized` to serialize matching across
    threads" because

- For the initial Milestone 1, the synchronized locks are simpler to test
  and reason about.
- It solves the problem of deterministic order matching without much 
  fine-grained locking.

## Pros and Cons of the Options

### Option A - Use a single dedicated writer thread fed by a ring buffer (inspired by the LMAX Disruptor paper)

- Description
  - Emulate the LMAX Disruptor, created by LMAX Exchange for low-latency, high-throughput inter-thread messaging.
- Pros
  - Since all the event objects in the ring buffer are pre-allocated, there is zero garbage collection overhead. 
- Cons
  - This is a very advanced strategy for milestone 1 requiring careful event-clearing logic (otherwise old object references in the objects can cause order book inconsistencies) and other ways to maintain throughput of millions of matches per second.
  - The ring buffer has the maximum memory allotted for its size and doesn't dynamically change its size based on order traffic. 

### Option B - Use a lock-free data structure and use Compare-And-Swap

- Description
  - A data structure where each read and write is done using CAS. All orders are matched and filled in FIFO order. 
- Pros
  - Since it uses CAS, it is guaranteed that at least one order is being matched at any given time. There is no case where all threads are waiting and none are making progress. 
- Cons
  - It is very hard to maintain total order of orders matched. This is very critical for this application since orders matched must be the same when replayed (e.g. for audit trails).
  - Multistep changes like cancellation and release of escrow funds must be atomic. CAS only guarantees atomicity for single steps. So, this option requires a lot of fine-grained locks.

### Option C - Use `synchronized` to serialize matching across threads

- Description
  - Each thread carrying an order request is synchronized on the matching service. The matching service alone manages updates to the order books.
- Pros
  - It will let me shard by symbol in the further milestones when I scale.
- Cons
  - The throughput is bound by per-order match time.
  - I am trading a simpler implementation (and testing) and determinism for no parallelism within a book.