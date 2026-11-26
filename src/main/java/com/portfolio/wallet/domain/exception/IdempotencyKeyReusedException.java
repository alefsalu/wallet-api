package com.portfolio.wallet.domain.exception;

public class IdempotencyKeyReusedException extends DomainException {

    public IdempotencyKeyReusedException(String message) {
        super(message, "IDEMPOTENCY_KEY_REUSED");
    }
}
