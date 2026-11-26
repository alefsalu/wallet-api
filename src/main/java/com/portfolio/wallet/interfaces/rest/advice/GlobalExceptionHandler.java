package com.portfolio.wallet.interfaces.rest.advice;

import com.portfolio.wallet.domain.exception.EmailAlreadyRegisteredException;
import com.portfolio.wallet.domain.exception.ForbiddenException;
import com.portfolio.wallet.domain.exception.IdempotencyKeyReusedException;
import com.portfolio.wallet.domain.exception.InsufficientFundsException;
import com.portfolio.wallet.domain.exception.InvalidMoneyException;
import com.portfolio.wallet.domain.exception.RateLimitedException;
import com.portfolio.wallet.domain.exception.SameWalletTransferException;
import com.portfolio.wallet.domain.exception.UnauthorizedException;
import com.portfolio.wallet.domain.exception.WalletNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_BASE = "https://api.wallet.portfolio.com/errors/";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetailResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ProblemDetailResponse.ValidationError> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(new ProblemDetailResponse.ValidationError(
                    fieldError.getField(),
                    fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value"));
        }

        ProblemDetailResponse response = new ProblemDetailResponse(
                TYPE_BASE + "VALIDATION_ERROR",
                "Validation Failed",
                HttpStatus.BAD_REQUEST.value(),
                "One or more fields failed validation",
                request.getRequestURI(),
                "VALIDATION_ERROR",
                errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
    }

    @ExceptionHandler(InvalidMoneyException.class)
    public ResponseEntity<ProblemDetailResponse> handleInvalidMoney(
            InvalidMoneyException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid Money Amount", ex.getMessage(), request);
    }

    @ExceptionHandler({UnauthorizedException.class, AuthenticationException.class})
    public ResponseEntity<ProblemDetailResponse> handleUnauthorized(
            Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized", ex.getMessage(), request);
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ProblemDetailResponse> handleForbidden(
            Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden", ex.getMessage(), request);
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ProblemDetailResponse> handleWalletNotFound(
            WalletNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "Wallet Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ProblemDetailResponse> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetailResponse> handleOptimisticLocking(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "CONCURRENT_MODIFICATION",
                "Concurrent Modification",
                "The resource was modified concurrently by another transaction. Please retry.",
                request);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ProblemDetailResponse> handleInsufficientFunds(
            InsufficientFundsException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_FUNDS", "Insufficient Funds", ex.getMessage(), request);
    }

    @ExceptionHandler(SameWalletTransferException.class)
    public ResponseEntity<ProblemDetailResponse> handleSameWalletTransfer(
            SameWalletTransferException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "SAME_WALLET_TRANSFER", "Invalid Transfer", ex.getMessage(), request);
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    public ResponseEntity<ProblemDetailResponse> handleIdempotencyKeyReused(
            IdempotencyKeyReusedException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IDEMPOTENCY_KEY_REUSED",
                "Idempotency Key Conflict",
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<ProblemDetailResponse> handleRateLimited(
            RateLimitedException ex, HttpServletRequest request) {
        ProblemDetailResponse response = new ProblemDetailResponse(
                TYPE_BASE + "RATE_LIMITED",
                "Too Many Requests",
                HttpStatus.TOO_MANY_REQUESTS.value(),
                ex.getMessage(),
                request.getRequestURI(),
                "RATE_LIMITED",
                null);

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetailResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid Request", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled server exception at {}", request.getRequestURI(), ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Internal Server Error",
                "An unexpected internal error occurred",
                request);
    }

    private ResponseEntity<ProblemDetailResponse> buildResponse(
            HttpStatus status, String code, String title, String detail, HttpServletRequest request) {
        ProblemDetailResponse response = new ProblemDetailResponse(
                TYPE_BASE + code,
                title,
                status.value(),
                detail,
                request.getRequestURI(),
                code,
                null);

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
    }
}
