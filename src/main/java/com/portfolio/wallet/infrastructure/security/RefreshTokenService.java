package com.portfolio.wallet.infrastructure.security;

import com.portfolio.wallet.domain.exception.UnauthorizedException;
import com.portfolio.wallet.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.portfolio.wallet.infrastructure.persistence.repository.SpringDataRefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService implements com.portfolio.wallet.application.port.output.RefreshTokenPort {

    private final SpringDataRefreshTokenRepository refreshTokenRepository;
    private final KeyProperties keyProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    

    public RefreshTokenService(
            SpringDataRefreshTokenRepository refreshTokenRepository,
            KeyProperties keyProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.keyProperties = keyProperties;
    }

    @Transactional
    public String createRefreshToken(UUID userId) {
        UUID familyId = UUID.randomUUID();
        return issueToken(userId, familyId);
    }

    @Transactional
    public RotatedToken rotateRefreshToken(String oldRawToken) {
        if (oldRawToken == null || oldRawToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String hash = computeSha256(oldRawToken);
        RefreshTokenJpaEntity existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (existing.isRevoked()) {
            // Token reuse detected! Revoke the entire family immediately!
            refreshTokenRepository.revokeFamily(existing.getFamilyId());
            throw new UnauthorizedException("Refresh token reuse detected: token family revoked");
        }

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Invalidate old token
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        // Issue new rotated token in the same family
        String newRawToken = issueToken(existing.getUserId(), existing.getFamilyId());
        return new RotatedToken(newRawToken, existing.getUserId());
    }

    @Transactional
    public void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String hash = computeSha256(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(entity -> {
            entity.setRevoked(true);
            refreshTokenRepository.save(entity);
        });
    }

    private String issueToken(UUID userId, UUID familyId) {
        byte[] randomBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String hash = computeSha256(rawToken);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(keyProperties.getRefreshTokenExpirationDays(), ChronoUnit.DAYS);

        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity(
                UUID.randomUUID(),
                userId,
                hash,
                familyId,
                false,
                expiresAt,
                now);

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    public String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }
}
