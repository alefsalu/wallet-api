package com.portfolio.wallet.domain.model;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) implements Comparable<UserId> {

    public UserId {
        Objects.requireNonNull(value, "User ID value cannot be null");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId of(String value) {
        Objects.requireNonNull(value, "User ID string cannot be null");
        return new UserId(UUID.fromString(value));
    }

    @Override
    public int compareTo(UserId o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
