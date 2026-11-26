package com.portfolio.wallet.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonRawValue;
import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String aggregateType,
        String aggregateId,
        UUID correlationId,
        @JsonRawValue String payload
) {}
