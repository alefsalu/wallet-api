package com.portfolio.wallet.domain.exception;

public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super(message, "FORBIDDEN");
    }
}
