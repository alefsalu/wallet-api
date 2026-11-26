package com.portfolio.wallet.domain.model;

import java.util.Objects;
import java.util.UUID;

public record TransactionId(UUID value) implements Comparable<TransactionId> {

    public TransactionId {
        Objects.requireNonNull(value, "Transaction ID value cannot be null");
    }

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID());
    }

    public static TransactionId of(UUID value) {
        return new TransactionId(value);
    }

    public static TransactionId of(String value) {
        Objects.requireNonNull(value, "Transaction ID string cannot be null");
        return new TransactionId(UUID.fromString(value));
    }

    @Override
    public int compareTo(TransactionId o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
