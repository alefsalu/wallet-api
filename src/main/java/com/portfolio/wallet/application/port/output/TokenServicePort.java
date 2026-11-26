package com.portfolio.wallet.application.port.output;

import java.util.List;
import java.util.UUID;

public interface TokenServicePort {
    String generateAccessToken(UUID userId, String email, List<String> roles);
    long getAccessTokenExpirationSeconds();
}
