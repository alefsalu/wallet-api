package com.portfolio.wallet.infrastructure.persistence.repository;

import com.portfolio.wallet.infrastructure.persistence.entity.TransactionJpaEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataTransactionRepository extends JpaRepository<TransactionJpaEntity, UUID> {

    Page<TransactionJpaEntity> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);
}
