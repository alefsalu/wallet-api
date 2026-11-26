package com.portfolio.wallet.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class KeyProperties {

    private String privateKeyPath = "keys/private.pem";
    private String publicKeyPath = "keys/public.pem";
    private String keyId = "wallet-dev-1";
    private String issuer = "wallet-api";
    private String audience = "portfolio";
    private long accessTokenExpirationSeconds = 900;
    private long refreshTokenExpirationDays = 7;

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getPublicKeyPath() {
        return publicKeyPath;
    }

    public void setPublicKeyPath(String publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public void setAccessTokenExpirationSeconds(long accessTokenExpirationSeconds) {
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationDays() {
        return refreshTokenExpirationDays;
    }

    public void setRefreshTokenExpirationDays(long refreshTokenExpirationDays) {
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }
}
