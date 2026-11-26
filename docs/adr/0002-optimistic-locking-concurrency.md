# ADR 0002: Optimistic Locking and Concurrency Control

## Context and Problem Statement

Financial ledger balances are subjected to concurrent modifications (multiple deposits, withdrawals, or transfers targeting the same wallet simultaneously). Pessimistic row locking (`SELECT FOR UPDATE`) causes long lock holding times, high database connection contention, and severe risks of deadlocks when two parties execute mutual transfers simultaneously (User A transfers to User B while User B transfers to User A).

## Decision Drivers

- High throughput on non-conflicting reads and operations.
- Strong consistency: negative balances and lost updates must be mathematically impossible.
- Deterministic behavior: prevent circular waits and database transaction deadlocks.

## Considered Options

- Pessimistic row locking (`SELECT FOR UPDATE`)
- Optimistic locking using version column (`@Version`)
- Distributed locks via Redis (Redlock)

## Decision Outcome

Chosen option: **Optimistic Locking with `@Version` column combined with Deterministic UUID Acquisition Order for Transfers**.

1. Wallets maintain a `version` BIGINT column. Any update increments version; concurrent modifications with stale versions trigger `OptimisticLockingFailureException`, translated to HTTP 409 `CONCURRENT_MODIFICATION`.
2. When executing transfers involving two wallets, acquisitions and database updates are sorted deterministically by Wallet UUID (`walletA.id().compareTo(walletB.id())`). This guarantees both transactions acquire row locks in identical physical sequence, eliminating circular waiting and PostgreSQL deadlocks.

### Positive Consequences

- Zero database deadlocks during mutual transfers.
- Non-blocking reads; optimal resource utilization under high read-to-write ratios.
- Clear RFC 7807 error signaling client to retry upon contention.

### Negative Consequences

- High contention on a single hot account requires client-side retry or queueing.
