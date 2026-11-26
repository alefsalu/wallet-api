package com.portfolio.wallet.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwksControllerTest {

    private JwksController jwksController;

    @BeforeEach
    void setUp() {
        KeyProperties properties = new KeyProperties();
        RsaKeyProvider rsaKeyProvider = new RsaKeyProvider(properties);
        rsaKeyProvider.init();
        jwksController = new JwksController(rsaKeyProvider);
    }

    @Test
    @DisplayName("Should expose JWKS containing RSA public key with kid=wallet-dev-1")
    @SuppressWarnings("unchecked")
    void shouldReturnJwks() {
        Map<String, Object> jwks = jwksController.getJwks();
        assertThat(jwks).containsKey("keys");

        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
        assertThat(keys).hasSize(1);

        Map<String, Object> key = keys.get(0);
        assertThat(key.get("kty")).isEqualTo("RSA");
        assertThat(key.get("kid")).isEqualTo("wallet-dev-1");
        assertThat(key.get("use")).isEqualTo("sig");
        assertThat(key.get("alg")).isEqualTo("RS256");
        assertThat(key).containsKey("n");
        assertThat(key).containsKey("e");
        assertThat(key).doesNotContainKey("d"); // Must never expose private key components!
    }
}
