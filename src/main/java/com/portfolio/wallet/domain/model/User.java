package com.portfolio.wallet.domain.model;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String name,
        String email,
        String passwordHash,
        String role,
        Instant createdAt) {

    public static User create(String name, String email, String passwordHash) {
        return new User(UUID.randomUUID(), name, email, passwordHash, "USER", Instant.now());
    }
}
