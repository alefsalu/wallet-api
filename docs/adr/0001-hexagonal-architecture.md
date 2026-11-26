# ADR 0001: Hexagonal Architecture (Ports & Adapters)

## Context and Problem Statement

A digital wallet requires strict isolation of financial business invariants (e.g., money bounds, overdraft prevention, invariant balance checks) from frameworks, web delivery protocols, and persistence mechanisms. Coupling domain rules directly to Spring annotations or JPA entities increases risk of regression, impedes unit testability, and leaks persistence concerns into business calculations.

## Decision Drivers

- Clean boundary isolation: domain logic must be 100% framework-free.
- Pure unit tests for financial mathematics without booting Spring contexts.
- Swappable persistence and messaging infrastructure without touching domain rules.
- Automated architectural enforcement via ArchUnit.

## Considered Options

- Layered Architecture (Controller -> Service -> DAO/Entity)
- Hexagonal Architecture (Ports and Adapters)
- Clean Architecture (Use Cases, Gateways, Entities)

## Decision Outcome

Chosen option: **Hexagonal Architecture (Ports and Adapters)**.

### Positive Consequences

- The `domain` package has zero external framework dependencies (validated by `HexagonalArchitectureTest`).
- Application services orchestrate domain aggregates via input ports (`TransferMoneyUseCase`) and interact with infrastructure through output ports (`WalletRepositoryPort`).
- Persistence models (`WalletJpaEntity`) can evolve or be tuned with JPA-specific annotations without impacting domain concepts.

### Negative Consequences

- Requires mapping boilerplate between JPA entities, domain aggregates, and REST DTOs.
- Additional abstraction layers require disciplined team adherence.
