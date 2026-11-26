package com.portfolio.wallet.infrastructure.persistence.repository;

import com.portfolio.wallet.infrastructure.persistence.entity.OutboxJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataOutboxRepository extends JpaRepository<OutboxJpaEntity, UUID> {

    List<OutboxJpaEntity> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();
}
