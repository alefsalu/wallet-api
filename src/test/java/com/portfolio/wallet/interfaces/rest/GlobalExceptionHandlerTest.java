package com.portfolio.wallet.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.wallet.domain.exception.EmailAlreadyRegisteredException;
import com.portfolio.wallet.domain.exception.ForbiddenException;
import com.portfolio.wallet.domain.exception.IdempotencyKeyReusedException;
import com.portfolio.wallet.domain.exception.InsufficientFundsException;
import com.portfolio.wallet.domain.exception.InvalidMoneyException;
import com.portfolio.wallet.domain.exception.RateLimitedException;
import com.portfolio.wallet.domain.exception.SameWalletTransferException;
import com.portfolio.wallet.domain.exception.UnauthorizedException;
import com.portfolio.wallet.domain.exception.WalletNotFoundException;
import com.portfolio.wallet.interfaces.rest.advice.GlobalExceptionHandler;
import com.portfolio.wallet.interfaces.rest.advice.ProblemDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
    }

    @Test
    @DisplayName("Should map domain exceptions to exact RFC 7807 problem details and status codes")
    void shouldMapExceptionsCorrectly() {
        // 400 VALIDATION_ERROR
        ResponseEntity<ProblemDetailResponse> r1 = handler.handleInvalidMoney(
                new InvalidMoneyException("Invalid scale"), request);
        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r1.getBody().code()).isEqualTo("VALIDATION_ERROR");

        // 401 UNAUTHORIZED
        ResponseEntity<ProblemDetailResponse> r2 = handler.handleUnauthorized(
                new UnauthorizedException("Bad token"), request);
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r2.getBody().code()).isEqualTo("UNAUTHORIZED");

        // 403 FORBIDDEN
        ResponseEntity<ProblemDetailResponse> r3 = handler.handleForbidden(
                new ForbiddenException("Access denied"), request);
        assertThat(r3.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(r3.getBody().code()).isEqualTo("FORBIDDEN");

        // 404 WALLET_NOT_FOUND
        ResponseEntity<ProblemDetailResponse> r4 = handler.handleWalletNotFound(
                new WalletNotFoundException("Wallet missing"), request);
        assertThat(r4.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r4.getBody().code()).isEqualTo("WALLET_NOT_FOUND");

        // 409 EMAIL_ALREADY_REGISTERED
        ResponseEntity<ProblemDetailResponse> r5 = handler.handleEmailAlreadyRegistered(
                new EmailAlreadyRegisteredException("Email taken"), request);
        assertThat(r5.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r5.getBody().code()).isEqualTo("EMAIL_ALREADY_REGISTERED");

        // 409 CONCURRENT_MODIFICATION
        ResponseEntity<ProblemDetailResponse> r6 = handler.handleOptimisticLocking(
                new OptimisticLockingFailureException("Version conflict"), request);
        assertThat(r6.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r6.getBody().code()).isEqualTo("CONCURRENT_MODIFICATION");

        // 422 INSUFFICIENT_FUNDS
        ResponseEntity<ProblemDetailResponse> r7 = handler.handleInsufficientFunds(
                new InsufficientFundsException("No funds"), request);
        assertThat(r7.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r7.getBody().code()).isEqualTo("INSUFFICIENT_FUNDS");

        // 422 SAME_WALLET_TRANSFER
        ResponseEntity<ProblemDetailResponse> r8 = handler.handleSameWalletTransfer(
                new SameWalletTransferException("Self transfer"), request);
        assertThat(r8.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r8.getBody().code()).isEqualTo("SAME_WALLET_TRANSFER");

        // 422 IDEMPOTENCY_KEY_REUSED
        ResponseEntity<ProblemDetailResponse> r9 = handler.handleIdempotencyKeyReused(
                new IdempotencyKeyReusedException("Payload changed"), request);
        assertThat(r9.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r9.getBody().code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");

        // 429 RATE_LIMITED
        ResponseEntity<ProblemDetailResponse> r10 = handler.handleRateLimited(
                new RateLimitedException("Too many calls", 45), request);
        assertThat(r10.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(r10.getBody().code()).isEqualTo("RATE_LIMITED");
        assertThat(r10.getHeaders().getFirst("Retry-After")).isEqualTo("45");
    }
}
