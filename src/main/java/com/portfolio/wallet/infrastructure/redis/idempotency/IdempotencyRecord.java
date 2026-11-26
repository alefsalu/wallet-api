package com.portfolio.wallet.infrastructure.redis.idempotency;

import java.io.Serializable;
import java.util.Map;

public record IdempotencyRecord(
        String requestHash,
        int responseStatus,
        String responseBody,
        Map<String, String> responseHeaders
) implements Serializable {}
