package com.portfolio.wallet.domain.exception;

public class EmailAlreadyRegisteredException extends DomainException {

    public EmailAlreadyRegisteredException(String message) {
        super(message, "EMAIL_ALREADY_REGISTERED");
    }
}
