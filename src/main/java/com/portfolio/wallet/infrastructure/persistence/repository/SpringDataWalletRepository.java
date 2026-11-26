package com.portfolio.wallet.infrastructure.persistence.repository;

import com.portfolio.wallet.infrastructure.persistence.entity.WalletJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataWalletRepository extends JpaRepository<WalletJpaEntity, UUID> {

    Optional<WalletJpaEntity> findByOwnerId(UUID ownerId);
}
