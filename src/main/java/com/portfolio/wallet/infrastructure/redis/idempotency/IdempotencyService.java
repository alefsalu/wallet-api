package com.portfolio.wallet.infrastructure.redis.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.wallet.domain.exception.IdempotencyKeyReusedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String buildRedisKey(UUID userId, String idempotencyKey) {
        return KEY_PREFIX + userId + ":" + idempotencyKey;
    }

    public String hashPayload(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public Optional<IdempotencyRecord> findRecord(String redisKey, String currentPayloadHash) {
        Object raw = redisTemplate.opsForValue().get(redisKey);
        if (raw == null) {
            return Optional.empty();
        }

        IdempotencyRecord record = objectMapper.convertValue(raw, IdempotencyRecord.class);

        if (!record.requestHash().equals(currentPayloadHash)) {
            throw new IdempotencyKeyReusedException(
                    "Idempotency key reused with different request payload");
        }

        return Optional.of(record);
    }

    public void storeRecord(String redisKey, String requestHash, int status, String body, Map<String, String> headers) {
        IdempotencyRecord record = new IdempotencyRecord(requestHash, status, body, headers);
        redisTemplate.opsForValue().set(redisKey, record, TTL);
    }
}
