package com.portfolio.wallet.interfaces.rest.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email
) {}
