# ADR 0003: Transactional Outbox Pattern with RabbitMQ Topic Exchanges

## Context and Problem Statement

When wallet state changes (deposit, withdrawal, transfer), downstream microservices (such as the Notification Service and Orders Service) must be notified via domain events. Directly publishing messages to RabbitMQ inside an active database transaction creates dual-write consistency hazards: if the broker publish succeeds but the database transaction subsequently rolls back, phantom events are emitted; conversely, if the database commits but the network blips before broker publishing, events are lost permanently.

## Decision Drivers

- At-least-once reliable delivery guarantee for all domain events.
- Zero dual-write inconsistencies between PostgreSQL and RabbitMQ.
- Independent resilience: temporary message broker downtime must not disrupt wallet API transactions.

## Considered Options

- Inline RabbitMQ publish inside Spring `@Transactional`
- Change Data Capture (CDC) via Debezium and Kafka
- Transactional Outbox Table with Scheduled Publisher Confirms Relay

## Decision Outcome

Chosen option: **Transactional Outbox Table with Scheduled Publisher Confirms Relay**.

1. When mutating wallet balances, domain events are serialized to JSON and persisted to the `outbox` table in the exact same database transaction.
2. A background scheduler (`OutboxRelayScheduler`) runs every 1000ms, batch-fetching up to 100 unpublished records.
3. The scheduler publishes messages to the `wallet.events` topic exchange with publisher confirms enabled. Once confirmed by RabbitMQ, `published_at` is stamped.
4. Any transient failures increment the record's `attempts` counter without blocking other valid events in the batch.

### Positive Consequences

- Absolute transactional consistency between database mutations and emitted events.
- Wallet mutations continue normally even if RabbitMQ is temporarily unreachable.
- Downstream services deduplicate by `eventId` (UUID).

### Negative Consequences

- Small eventual consistency delay (typically < 1000ms) between database commit and message delivery.
- Database storage overhead for maintaining the outbox table.
