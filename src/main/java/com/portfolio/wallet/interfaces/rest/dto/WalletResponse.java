package com.portfolio.wallet.interfaces.rest.dto;

import com.portfolio.wallet.domain.model.Wallet;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID ownerId,
        MoneyDto balance,
        Instant createdAt
) {
    public static WalletResponse fromDomain(Wallet wallet) {
        return new WalletResponse(
                wallet.id().value(),
                wallet.ownerId().value(),
                MoneyDto.fromDomain(wallet.balance()),
                wallet.createdAt());
    }
}
