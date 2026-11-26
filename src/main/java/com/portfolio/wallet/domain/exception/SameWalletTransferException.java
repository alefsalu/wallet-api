package com.portfolio.wallet.domain.exception;

public class SameWalletTransferException extends DomainException {

    public SameWalletTransferException(String message) {
        super(message, "SAME_WALLET_TRANSFER");
    }
}
