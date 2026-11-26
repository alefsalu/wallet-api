# ADR 0004: Asymmetric JWT Signing with RS256 and JWKS Exposure

## Context and Problem Statement

In a multi-service distributed architecture (Wallet API, Orders Service, Notification Service), incoming requests must be authenticated securely. Symmetric signing (HS256) requires sharing the HMAC secret with all downstream resource servers, posing a significant security vulnerability: if any consumer is compromised, the secret can be used to forge unauthorized access tokens.

## Decision Drivers

- Asymmetric separation of concerns: only Wallet API holds the private signing key.
- Downstream resource servers validate tokens locally without network roundtrips to an authentication server.
- Standard key discovery protocol via RFC 7517 JSON Web Key Sets (JWKS).

## Considered Options

- Symmetric HMAC-SHA256 (HS256)
- Asymmetric RSA-SHA256 (RS256) with JWKS endpoint
- Centralized token introspection endpoint (`/oauth/check_token`)

## Decision Outcome

Chosen option: **Asymmetric RS256 with Public JWKS (`/.well-known/jwks.json`)**.

1. Wallet API holds a 2048-bit RSA private key and signs access tokens with RS256 (`kid="wallet-dev-1"`).
2. Wallet API exposes public key coordinates (`n`, `e`) at `GET /.well-known/jwks.json`.
3. Downstream resource servers cache the public key and validate tokens entirely in-memory with zero runtime latency.
4. Refresh tokens are opaque 256-bit cryptographically random tokens stored as SHA-256 hashes, rotated on every usage, with immediate revocation of the whole token family upon reuse detection.

### Positive Consequences

- Zero risk of token forgery from compromised downstream resource servers.
- High performance verification across the distributed portfolio.
- Adherence to standard OAuth2 / OIDC specifications.

### Negative Consequences

- Slightly larger token size compared to symmetric HS256 tokens.
- Computational overhead of RSA signing (mitigated by modern CPU hardware acceleration).
