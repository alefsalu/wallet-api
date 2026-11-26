package com.portfolio.wallet.application.port.output;

import com.portfolio.wallet.domain.event.DomainEvent;
import java.util.UUID;

public interface OutboxRepositoryPort {

    void save(DomainEvent event);

    void save(DomainEvent event, UUID correlationId);
}
