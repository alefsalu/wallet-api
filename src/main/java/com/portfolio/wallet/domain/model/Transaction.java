package com.portfolio.wallet.domain.model;

import java.time.Instant;
import java.util.Objects;

public class Transaction {

    private final TransactionId id;
    private final WalletId walletId;
    private final TransactionType type;
    private final Money amount;
    private final Money balanceAfter;
    private final WalletId counterpartyWalletId;
    private final String description;
    private final Instant createdAt;

    public Transaction(
            TransactionId id,
            WalletId walletId,
            TransactionType type,
            Money amount,
            Money balanceAfter,
            WalletId counterpartyWalletId,
            String description,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Transaction ID is required");
        this.walletId = Objects.requireNonNull(walletId, "Wallet ID is required");
        this.type = Objects.requireNonNull(type, "Transaction Type is required");
        this.amount = Objects.requireNonNull(amount, "Amount is required");
        this.balanceAfter = Objects.requireNonNull(balanceAfter, "Balance after mutation is required");
        this.counterpartyWalletId = counterpartyWalletId;
        this.description = description;
        this.createdAt = Objects.requireNonNull(createdAt, "Created at timestamp is required");
    }

    public static Transaction create(
            WalletId walletId,
            TransactionType type,
            Money amount,
            Money balanceAfter,
            WalletId counterpartyWalletId,
            String description) {
        return new Transaction(
                TransactionId.generate(),
                walletId,
                type,
                amount,
                balanceAfter,
                counterpartyWalletId,
                description,
                Instant.now());
    }

    public TransactionId getId() {
        return id;
    }

    public WalletId getWalletId() {
        return walletId;
    }

    public TransactionType getType() {
        return type;
    }

    public Money getAmount() {
        return amount;
    }

    public Money getBalanceAfter() {
        return balanceAfter;
    }

    public WalletId getCounterpartyWalletId() {
        return counterpartyWalletId;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
