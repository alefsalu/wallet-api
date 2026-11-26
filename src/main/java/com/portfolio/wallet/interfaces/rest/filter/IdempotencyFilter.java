package com.portfolio.wallet.interfaces.rest.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.wallet.domain.exception.IdempotencyKeyReusedException;
import com.portfolio.wallet.infrastructure.redis.idempotency.IdempotencyRecord;
import com.portfolio.wallet.infrastructure.redis.idempotency.IdempotencyService;
import com.portfolio.wallet.interfaces.rest.advice.ProblemDetailResponse;
import com.portfolio.wallet.interfaces.rest.interceptor.IdempotencyInterceptor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (!IdempotencyInterceptor.isIdempotentRoute(uri) || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(IdempotencyInterceptor.IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            writeProblemResponse(
                    response,
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Validation Failed",
                    "Header 'Idempotency-Key' is required for this operation",
                    uri);
            return;
        }

        UUID keyUuid;
        try {
            keyUuid = UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException e) {
            writeProblemResponse(
                    response,
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Validation Failed",
                    "Header 'Idempotency-Key' must be a valid UUID format",
                    uri);
            return;
        }

        ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);

        // Read body to populate cache
        byte[] bodyBytes = cachingRequest.getInputStream().readAllBytes();
        String payloadHash = idempotencyService.hashPayload(bodyBytes);

        UUID userId = extractUserId();
        String redisKey = idempotencyService.buildRedisKey(userId, keyUuid.toString());

        try {
            Optional<IdempotencyRecord> existing = idempotencyService.findRecord(redisKey, payloadHash);
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();
                response.setStatus(record.responseStatus());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader(IdempotencyInterceptor.IDEMPOTENT_REPLAYED_HEADER, "true");
                if (record.responseHeaders() != null) {
                    record.responseHeaders().forEach(response::setHeader);
                }
                response.getWriter().write(record.responseBody());
                return;
            }
        } catch (IdempotencyKeyReusedException e) {
            writeProblemResponse(
                    response,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency Key Conflict",
                    e.getMessage(),
                    uri);
            return;
        }

        filterChain.doFilter(cachingRequest, cachingResponse);

        int status = cachingResponse.getStatus();
        if (status >= 200 && status < 300) {
            byte[] responseContent = cachingResponse.getContentAsByteArray();
            String responseBody = new String(responseContent, StandardCharsets.UTF_8);
            Map<String, String> headers = new HashMap<>();
            idempotencyService.storeRecord(redisKey, payloadHash, status, responseBody, headers);
        }

        cachingResponse.copyBodyToResponse();
    }

    private UUID extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            try {
                return UUID.fromString(jwt.getSubject());
            } catch (Exception ignored) {
            }
        }
        return UUID.nameUUIDFromBytes("anonymous".getBytes(StandardCharsets.UTF_8));
    }

    private void writeProblemResponse(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String title,
            String detail,
            String uri)
            throws IOException {
        ProblemDetailResponse problem = new ProblemDetailResponse(
                "https://api.wallet.portfolio.com/errors/" + code,
                title,
                status.value(),
                detail,
                uri,
                code,
                null);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
