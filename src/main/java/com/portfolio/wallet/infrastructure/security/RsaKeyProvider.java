package com.portfolio.wallet.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    private final KeyProperties properties;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private RSAKey rsaJwk;

    public RsaKeyProvider(KeyProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        File privFile = new File(properties.getPrivateKeyPath());
        File pubFile = new File(properties.getPublicKeyPath());

        if (privFile.exists() && pubFile.exists()) {
            try {
                this.privateKey = loadPrivateKey(privFile);
                this.publicKey = loadPublicKey(pubFile);
                log.info("Loaded RSA keys from {} and {}", privFile.getPath(), pubFile.getPath());
            } catch (Exception e) {
                log.warn("Failed to load RSA keys from files. Generating ephemeral keypair: {}", e.getMessage());
                generateEphemeralKeyPair();
            }
        } else {
            log.info("RSA key files not found. Generating ephemeral 2048-bit RSA keypair.");
            generateEphemeralKeyPair();
        }

        this.rsaJwk = new RSAKey.Builder(this.publicKey)
                .privateKey(this.privateKey)
                .keyID(properties.getKeyId())
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                .build();
    }

    private void generateEphemeralKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate ephemeral RSA keypair", e);
        }
    }

    private RSAPrivateKey loadPrivateKey(File file) throws Exception {
        String pem = Files.readString(file.toPath());
        String clean = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(clean);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private RSAPublicKey loadPublicKey(File file) throws Exception {
        String pem = Files.readString(file.toPath());
        String clean = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(clean);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAKey getRsaJwk() {
        return rsaJwk;
    }

    public JWKSet getJwkSet() {
        return new JWKSet(rsaJwk.toPublicJWK());
    }

    public String getKeyId() {
        return properties.getKeyId();
    }
}
