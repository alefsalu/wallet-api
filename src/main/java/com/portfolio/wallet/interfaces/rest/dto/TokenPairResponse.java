package com.portfolio.wallet.interfaces.rest.dto;

public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {}
