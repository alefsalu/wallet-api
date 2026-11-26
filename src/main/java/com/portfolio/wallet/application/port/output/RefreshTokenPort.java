package com.portfolio.wallet.application.port.output;

import java.util.UUID;

public interface RefreshTokenPort {
    record RotatedToken(String rawToken, UUID userId) {}
    String createRefreshToken(UUID userId);
    RotatedToken rotateRefreshToken(String rawRefreshToken);
    void revokeToken(String rawRefreshToken);
}
