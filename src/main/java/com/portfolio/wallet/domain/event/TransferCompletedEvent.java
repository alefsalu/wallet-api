package com.portfolio.wallet.domain.event;

import com.portfolio.wallet.domain.model.Money;
import java.time.Instant;
import java.util.UUID;

public record TransferCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String destinationWalletId,
        Money amount,
        String description,
        UUID correlationId)
        implements DomainEvent {

    public static final String EVENT_TYPE = "wallet.transfer.completed.v1";
    public static final String AGGREGATE_TYPE = "Wallet";

    public TransferCompletedEvent(
            String sourceWalletId,
            String destinationWalletId,
            Money amount,
            String description,
            UUID correlationId) {
        this(
                UUID.randomUUID(),
                Instant.now(),
                sourceWalletId,
                destinationWalletId,
                amount,
                description,
                correlationId);
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
