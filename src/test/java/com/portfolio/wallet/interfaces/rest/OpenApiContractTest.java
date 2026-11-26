package com.portfolio.wallet.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenApiContractTest {

    @Test
    @DisplayName("Should verify openapi/openapi.yaml exists and defines required API endpoints")
    void shouldVerifyOpenApiContract() throws Exception {
        File specFile = new File("openapi/openapi.yaml");
        assertThat(specFile).exists();

        String yaml = Files.readString(specFile.toPath());
        assertThat(yaml).isNotBlank();

        // Verify endpoints
        assertThat(yaml).contains("/api/v1/auth/register");
        assertThat(yaml).contains("/api/v1/auth/login");
        assertThat(yaml).contains("/api/v1/auth/refresh");
        assertThat(yaml).contains("/api/v1/auth/logout");
        assertThat(yaml).contains("/api/v1/wallets/me");
        assertThat(yaml).contains("/api/v1/wallets/me/transactions");
        assertThat(yaml).contains("/api/v1/wallets/me/deposits");
        assertThat(yaml).contains("/api/v1/wallets/me/withdrawals");
        assertThat(yaml).contains("/api/v1/transfers");

        // Verify RFC 7807 and Security
        assertThat(yaml).contains("ProblemDetailResponse");
        assertThat(yaml).contains("Idempotency-Key");
        assertThat(yaml).contains("bearerAuth");
    }
}
