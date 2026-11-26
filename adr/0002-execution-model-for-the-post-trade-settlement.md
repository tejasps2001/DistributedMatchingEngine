# Select the execution model for the post-trade settlement (Retroactive)

- Date: 2026-11-2026

## Context and Problem statement

Post an order's execution, the user's wallets must be updated with the appropriate balances and shares. The question is whether this settlement needs to be done on the matching thread.

## Considered options

- Option A - Settle synchronously inside the match
- Option B - Settle asynchronously using a thread pool

## Decision Outcome

Chosen option: "Option B - Settle asynchronously using a thread pool" because

- It protects the core matching latency. The settlement can happen safely later just like in T+1.