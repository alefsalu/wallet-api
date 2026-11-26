package com.portfolio.wallet.interfaces.rest.dto;

import com.portfolio.wallet.domain.model.Transaction;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String type,
        MoneyDto amount,
        MoneyDto balanceAfter,
        UUID counterpartyWalletId,
        String description,
        Instant createdAt
) {
    public static TransactionResponse fromDomain(Transaction tx) {
        return new TransactionResponse(
                tx.getId().value(),
                tx.getType().name(),
                MoneyDto.fromDomain(tx.getAmount()),
                MoneyDto.fromDomain(tx.getBalanceAfter()),
                tx.getCounterpartyWalletId() != null ? tx.getCounterpartyWalletId().value() : null,
                tx.getDescription(),
                tx.getCreatedAt());
    }
}
