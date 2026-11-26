# Wallet API Architecture

## C4 Context Diagram

```mermaid
C4Context
    title System Context diagram for Wallet API

    Person(user, "User / Client", "Customer interacting with their digital wallet")
    System(walletApi, "Wallet API", "Handles authentication, account balances, ledger entries, and atomic transfers")
    System_Ext(ordersService, "Orders Service", "Validates customer balances and consumes wallet events")
    System_Ext(notificationService, "Notification Service", "Receives event notifications to dispatch emails/SMS")
    System_Ext(dashboard, "Wallet Dashboard", "Single Page Application (React / Vite) for end users")

    Rel(user, dashboard, "Views balances, performs transfers", "HTTPS")
    Rel(dashboard, walletApi, "Executes wallet transactions", "JSON/REST/HTTPS (JWT RS256)")
    Rel(ordersService, walletApi, "Validates JWTs against JWKS", "GET /.well-known/jwks.json")
    Rel(walletApi, notificationService, "Publishes domain events (e.g. wallet.transfer.completed.v1)", "AMQP / Topic Exchange")
```

---

## C4 Container Diagram

```mermaid
C4Container
    title Container diagram for Wallet API

    Container(spa, "Single-Page Application", "React, TypeScript", "Provides digital wallet UI to customers")
    
    Container_Boundary(b1, "Wallet Service Boundary") {
        Container(api, "API Application", "Java 21, Spring Boot 3", "Provides REST APIs, enforces domain logic, verifies idempotency and JWTs")
        ContainerDb(db, "PostgreSQL Database", "PostgreSQL 16", "Stores users, wallets, ledger transactions, and transactional outbox table")
        ContainerDb(redis, "Distributed Cache", "Redis 7", "Stores distributed idempotency response caches (TTL 24h) and rate limiting sliding windows")
        Container(rabbitmq, "Message Broker", "RabbitMQ 3.13", "Routes domain events to consumers via topic exchanges")
    }

    Rel(spa, api, "API calls", "JSON/REST/HTTPS")
    Rel(api, db, "Reads & writes wallets, ledger and outbox", "JDBC / JPA")
    Rel(api, redis, "Checks/sets idempotency and rate limits", "Lettuce / RESP")
    Rel(api, rabbitmq, "Relays outbox events with publisher confirms", "AMQP")
```

---

## Hexagonal Package Structure

The codebase strictly adheres to Hexagonal Architecture (Ports and Adapters):

```
com.portfolio.wallet
├── domain
│   ├── event          # Domain events (e.g. TransferCompletedEvent)
│   ├── exception      # Pure domain exceptions (InsufficientFundsException, SameWalletTransferException)
│   └── model          # Core aggregates & value objects (Wallet, Money, Transaction)
├── application
│   ├── port
│   │   ├── input      # Use case interfaces (DepositUseCase, TransferMoneyUseCase)
│   │   └── output     # Outbound SPI ports (WalletRepositoryPort, OutboxRepositoryPort)
│   └── service        # Application use case orchestrators
├── infrastructure
│   ├── config         # Spring Security, Web MVC, OpenAPI beans
│   ├── messaging      # RabbitMQ configuration and OutboxRelayScheduler
│   ├── observability  # Actuator /health/live, /health/ready, /metrics
│   ├── persistence    # JPA entities, Flyway migrations, Spring Data repositories, adapters
│   ├── redis          # RedisTemplate, IdempotencyService, RateLimitingFilter
│   └── security       # RSA Key Provider, JWKS controller, JWT RS256 issuer, PasswordHasher
└── interfaces
    └── rest
        ├── advice     # RFC 7807 GlobalExceptionHandler
        ├── controller # REST controllers (AuthController, WalletQueryController, etc.)
        ├── dto        # Request and response contract DTOs
        ├── filter     # CorrelationIdFilter, IdempotencyFilter
        └── interceptor # IdempotencyInterceptor
```
