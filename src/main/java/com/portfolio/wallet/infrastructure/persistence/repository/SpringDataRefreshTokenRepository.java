package com.portfolio.wallet.infrastructure.persistence.repository;

import com.portfolio.wallet.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenJpaEntity> findByFamilyId(UUID familyId);

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity r SET r.isRevoked = true WHERE r.familyId = :familyId")
    void revokeFamily(UUID familyId);
}
