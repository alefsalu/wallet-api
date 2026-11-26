package com.portfolio.wallet.domain.model;

import com.portfolio.wallet.domain.event.DepositCompletedEvent;
import com.portfolio.wallet.domain.event.DomainEvent;
import com.portfolio.wallet.domain.event.TransferCompletedEvent;
import com.portfolio.wallet.domain.event.WithdrawalCompletedEvent;
import com.portfolio.wallet.domain.exception.InsufficientFundsException;
import com.portfolio.wallet.domain.exception.SameWalletTransferException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Wallet {

    private final WalletId id;
    private final UserId ownerId;
    private Money balance;
    private long version;
    private final Instant createdAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public record TransferResult(Transaction senderTransaction, Transaction receiverTransaction) {}

    private Wallet(WalletId id, UserId ownerId, Money balance, long version, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Wallet ID cannot be null");
        this.ownerId = Objects.requireNonNull(ownerId, "Owner ID cannot be null");
        this.balance = Objects.requireNonNull(balance, "Balance cannot be null");
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
    }

    public static Wallet create(UserId ownerId) {
        return new Wallet(
                WalletId.generate(),
                ownerId,
                Money.zero(),
                0L,
                Instant.now());
    }

    public static Wallet reconstitute(
            WalletId id,
            UserId ownerId,
            Money balance,
            long version,
            Instant createdAt) {
        return new Wallet(id, ownerId, balance, version, createdAt);
    }

    public Transaction deposit(Money amount) {
        return deposit(amount, UUID.randomUUID());
    }

    public Transaction deposit(Money amount, UUID correlationId) {
        Objects.requireNonNull(amount, "Deposit amount cannot be null");
        this.balance = this.balance.add(amount);

        Transaction transaction = Transaction.create(
                this.id,
                TransactionType.DEPOSIT,
                amount,
                this.balance,
                null,
                "Deposit of " + amount.amount() + " " + amount.currency());

        this.domainEvents.add(new DepositCompletedEvent(
                this.id.toString(),
                amount,
                this.balance,
                correlationId != null ? correlationId : UUID.randomUUID()));

        return transaction;
    }

    public Transaction withdraw(Money amount) {
        return withdraw(amount, UUID.randomUUID());
    }

    public Transaction withdraw(Money amount, UUID correlationId) {
        Objects.requireNonNull(amount, "Withdrawal amount cannot be null");
        if (this.balance.isLessThan(amount)) {
            throw new InsufficientFundsException(
                    "Insufficient balance. Current: " + balance.amount() + ", required: " + amount.amount());
        }

        this.balance = this.balance.subtract(amount);

        Transaction transaction = Transaction.create(
                this.id,
                TransactionType.WITHDRAWAL,
                amount,
                this.balance,
                null,
                "Withdrawal of " + amount.amount() + " " + amount.currency());

        this.domainEvents.add(new WithdrawalCompletedEvent(
                this.id.toString(),
                amount,
                this.balance,
                correlationId != null ? correlationId : UUID.randomUUID()));

        return transaction;
    }

    public TransferResult transferTo(Wallet destination, Money amount, String description) {
        return transferTo(destination, amount, description, UUID.randomUUID());
    }

    public TransferResult transferTo(Wallet destination, Money amount, String description, UUID correlationId) {
        Objects.requireNonNull(destination, "Destination wallet cannot be null");
        Objects.requireNonNull(amount, "Transfer amount cannot be null");

        if (this.id.equals(destination.id())) {
            throw new SameWalletTransferException("Cannot transfer to the same wallet");
        }

        if (this.balance.isLessThan(amount)) {
            throw new InsufficientFundsException(
                    "Insufficient funds for transfer. Available: " + balance.amount() + ", requested: " + amount.amount());
        }

        this.balance = this.balance.subtract(amount);
        destination.balance = destination.balance.add(amount);

        Transaction senderTx = Transaction.create(
                this.id,
                TransactionType.TRANSFER_OUT,
                amount,
                this.balance,
                destination.id(),
                description != null ? description : "Transfer to " + destination.id());

        Transaction receiverTx = Transaction.create(
                destination.id(),
                TransactionType.TRANSFER_IN,
                amount,
                destination.balance,
                this.id,
                description != null ? description : "Transfer from " + this.id());

        this.domainEvents.add(new TransferCompletedEvent(
                this.id.toString(),
                destination.id().toString(),
                amount,
                description,
                correlationId != null ? correlationId : UUID.randomUUID()));

        return new TransferResult(senderTx, receiverTx);
    }

    public WalletId id() {
        return id;
    }

    public WalletId getId() {
        return id;
    }

    public UserId ownerId() {
        return ownerId;
    }

    public UserId getOwnerId() {
        return ownerId;
    }

    public Money balance() {
        return balance;
    }

    public Money getBalance() {
        return balance;
    }

    public long version() {
        return version;
    }

    public long getVersion() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }
}
