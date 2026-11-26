package com.portfolio.wallet.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.wallet.application.port.output.OutboxRepositoryPort;
import com.portfolio.wallet.domain.event.DomainEvent;
import com.portfolio.wallet.infrastructure.persistence.entity.OutboxJpaEntity;
import com.portfolio.wallet.infrastructure.persistence.repository.SpringDataOutboxRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OutboxPersistenceAdapter implements OutboxRepositoryPort {

    private final SpringDataOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxPersistenceAdapter(SpringDataOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(DomainEvent event) {
        save(event, UUID.randomUUID());
    }

    @Override
    public void save(DomainEvent event, UUID correlationId) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            OutboxJpaEntity entity = new OutboxJpaEntity(
                    event.eventId(),
                    event.eventType(),
                    event.aggregateType(),
                    event.aggregateId(),
                    correlationId != null ? correlationId : UUID.randomUUID(),
                    jsonPayload,
                    event.occurredAt(),
                    null,
                    0);
            outboxRepository.save(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize domain event to outbox JSON", e);
        }
    }
}
