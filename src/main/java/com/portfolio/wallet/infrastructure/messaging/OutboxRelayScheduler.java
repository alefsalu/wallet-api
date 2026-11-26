package com.portfolio.wallet.infrastructure.messaging;

import com.portfolio.wallet.infrastructure.persistence.entity.OutboxJpaEntity;
import com.portfolio.wallet.infrastructure.persistence.repository.SpringDataOutboxRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final SpringDataOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxRelayScheduler(
            SpringDataOutboxRepository outboxRepository,
            RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void relayEvents() {
        List<OutboxJpaEntity> pending = outboxRepository.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();
        if (pending.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox records to relay", pending.size());

        for (OutboxJpaEntity record : pending) {
            publishSingleRecord(record);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishSingleRecord(OutboxJpaEntity record) {
        String corrId = record.getCorrelationId().toString();
        MDC.put("correlationId", corrId);

        try {
            EventEnvelope envelope = new EventEnvelope(
                    record.getId(),
                    record.getEventType(),
                    record.getOccurredAt(),
                    record.getAggregateType(),
                    record.getAggregateId(),
                    record.getCorrelationId(),
                    record.getPayload());

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE_WALLET_EVENTS,
                    record.getEventType(),
                    envelope);

            record.setPublishedAt(Instant.now());
            outboxRepository.save(record);
            log.info("Relayed outbox event {} of type {} to RabbitMQ", record.getId(), record.getEventType());
        } catch (Exception e) {
            // Review Focus #5: Log failure with correlationId, increment attempts, and do not halt batch
            record.setAttempts(record.getAttempts() + 1);
            outboxRepository.save(record);
            log.error("Failed to relay outbox event {} to RabbitMQ. Attempts incremented to {}: {}",
                    record.getId(), record.getAttempts(), e.getMessage());
        } finally {
            MDC.remove("correlationId");
        }
    }
}
