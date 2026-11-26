package com.portfolio.wallet.domain.exception;

public class WalletNotFoundException extends DomainException {

    public WalletNotFoundException(String message) {
        super(message, "WALLET_NOT_FOUND");
    }
}
