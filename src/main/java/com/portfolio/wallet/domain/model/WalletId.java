package com.portfolio.wallet.domain.model;

import java.util.Objects;
import java.util.UUID;

public record WalletId(UUID value) implements Comparable<WalletId> {

    public WalletId {
        Objects.requireNonNull(value, "Wallet ID value cannot be null");
    }

    public static WalletId generate() {
        return new WalletId(UUID.randomUUID());
    }

    public static WalletId of(UUID value) {
        return new WalletId(value);
    }

    public static WalletId of(String value) {
        Objects.requireNonNull(value, "Wallet ID string cannot be null");
        return new WalletId(UUID.fromString(value));
    }

    @Override
    public int compareTo(WalletId o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
