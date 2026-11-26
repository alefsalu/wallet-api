package com.portfolio.wallet.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService implements com.portfolio.wallet.application.port.output.TokenServicePort {

    private final RsaKeyProvider rsaKeyProvider;
    private final KeyProperties keyProperties;

    public JwtTokenService(RsaKeyProvider rsaKeyProvider, KeyProperties keyProperties) {
        this.rsaKeyProvider = rsaKeyProvider;
        this.keyProperties = keyProperties;
    }

    public String generateAccessToken(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(keyProperties.getAccessTokenExpirationSeconds());

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(keyProperties.getIssuer())
                .audience(keyProperties.getAudience())
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiry))
                .jwtID(UUID.randomUUID().toString())
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKeyProvider.getKeyId())
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claimsSet);

        try {
            RSASSASigner signer = new RSASSASigner(rsaKeyProvider.getPrivateKey());
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT with RS256", e);
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return keyProperties.getAccessTokenExpirationSeconds();
    }
}
