package com.portfolio.wallet.domain.event;

import com.portfolio.wallet.domain.model.Money;
import java.time.Instant;
import java.util.UUID;

public record DepositCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        Money amount,
        Money balanceAfter,
        UUID correlationId)
        implements DomainEvent {

    public static final String EVENT_TYPE = "wallet.deposit.completed.v1";
    public static final String AGGREGATE_TYPE = "Wallet";

    public DepositCompletedEvent(String aggregateId, Money amount, Money balanceAfter, UUID correlationId) {
        this(UUID.randomUUID(), Instant.now(), aggregateId, amount, balanceAfter, correlationId);
    }

    @Override
    public String aggregateType() {
        return AGGREGATE_TYPE;
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}
