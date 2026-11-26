package com.portfolio.wallet.infrastructure.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher implements com.portfolio.wallet.application.port.output.PasswordHasherPort {

    private final PasswordEncoder encoder;

    public PasswordHasher() {
        // saltLength: 16, hashLength: 32, parallelism: 1, memory: 4096 (4MB), iterations: 3
        this.encoder = new Argon2PasswordEncoder(16, 32, 1, 4096, 3);
    }

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }
}
