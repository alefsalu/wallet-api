package com.portfolio.wallet.interfaces.rest.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String IDEMPOTENT_REPLAYED_HEADER = "Idempotent-Replayed";

    public static void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Header 'Idempotency-Key' is required for this operation");
        }
        try {
            UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Header 'Idempotency-Key' must be a valid UUID");
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (isIdempotentRoute(uri) && "POST".equalsIgnoreCase(request.getMethod())) {
            String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
            validateIdempotencyKey(key);
        }
        return true;
    }

    public static boolean isIdempotentRoute(String uri) {
        return uri != null && (uri.endsWith("/wallets/me/deposits")
                || uri.endsWith("/wallets/me/withdrawals")
                || uri.endsWith("/transfers"));
    }
}
