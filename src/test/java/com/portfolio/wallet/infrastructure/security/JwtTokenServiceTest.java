package com.portfolio.wallet.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private RsaKeyProvider rsaKeyProvider;

    @BeforeEach
    void setUp() {
        KeyProperties properties = new KeyProperties();
        rsaKeyProvider = new RsaKeyProvider(properties);
        rsaKeyProvider.init();
        jwtTokenService = new JwtTokenService(rsaKeyProvider, properties);
    }

    @Test
    @DisplayName("Should generate valid RS256 JWT with correct claims and expiration")
    void shouldGenerateValidJwt() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "alice@example.com";
        List<String> roles = List.of("USER");

        String token = jwtTokenService.generateAccessToken(userId, email, roles);
        assertThat(token).isNotBlank();

        SignedJWT signedJWT = SignedJWT.parse(token);
        assertThat(signedJWT.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(signedJWT.getHeader().getKeyID()).isEqualTo("wallet-dev-1");

        assertThat(signedJWT.getJWTClaimsSet().getIssuer()).isEqualTo("wallet-api");
        assertThat(signedJWT.getJWTClaimsSet().getAudience()).containsExactly("portfolio");
        assertThat(signedJWT.getJWTClaimsSet().getSubject()).isEqualTo(userId.toString());
        assertThat(signedJWT.getJWTClaimsSet().getStringClaim("email")).isEqualTo(email);
        assertThat(signedJWT.getJWTClaimsSet().getStringListClaim("roles")).containsExactly("USER");
        assertThat(signedJWT.getJWTClaimsSet().getJWTID()).isNotNull();

        long diffSeconds = (signedJWT.getJWTClaimsSet().getExpirationTime().getTime()
                - signedJWT.getJWTClaimsSet().getIssueTime().getTime()) / 1000;
        assertThat(diffSeconds).isEqualTo(900L);
    }
}
