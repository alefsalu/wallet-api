package com.portfolio.wallet.infrastructure.redis.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.wallet.interfaces.rest.advice.ProblemDetailResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final int LOGIN_LIMIT_PER_MINUTE = 5;
    private static final int AUTHENTICATED_LIMIT_PER_MINUTE = 100;

    public RateLimitingFilter(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri.startsWith("/health") || uri.startsWith("/metrics") || uri.startsWith("/.well-known") || uri.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        long currentMinute = Instant.now().getEpochSecond() / 60;
        long secondsUntilNextMinute = 60 - (Instant.now().getEpochSecond() % 60);

        String rateLimitKey;
        int maxAllowed;

        if (uri.endsWith("/auth/login") && "POST".equalsIgnoreCase(method)) {
            String clientIp = getClientIp(request);
            rateLimitKey = "ratelimit:login:" + clientIp + ":" + currentMinute;
            maxAllowed = LOGIN_LIMIT_PER_MINUTE;
        } else {
            String clientIdentifier = getClientIdentifier(request);
            rateLimitKey = "ratelimit:api:" + clientIdentifier + ":" + currentMinute;
            maxAllowed = AUTHENTICATED_LIMIT_PER_MINUTE;
        }

        Long currentCount = stringRedisTemplate.opsForValue().increment(rateLimitKey);
        if (currentCount != null && currentCount == 1) {
            stringRedisTemplate.expire(rateLimitKey, Duration.ofSeconds(120));
        }

        if (currentCount != null && currentCount > maxAllowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(secondsUntilNextMinute));

            ProblemDetailResponse problem = new ProblemDetailResponse(
                    "https://api.wallet.portfolio.com/errors/RATE_LIMITED",
                    "Too Many Requests",
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Rate limit exceeded. Try again in " + secondsUntilNextMinute + " seconds.",
                    uri,
                    "RATE_LIMITED",
                    null);

            response.getWriter().write(objectMapper.writeValueAsString(problem));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIdentifier(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return "user:" + jwt.getSubject();
        }
        return "ip:" + getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
