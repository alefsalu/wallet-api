package com.portfolio.wallet.domain.exception;

public class InvalidMoneyException extends DomainException {

    public InvalidMoneyException(String message) {
        super(message, "VALIDATION_ERROR");
    }
}
