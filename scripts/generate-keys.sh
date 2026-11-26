#!/usr/bin/env bash
set -euo pipefail

KEYS_DIR="${1:-keys}"
mkdir -p "$KEYS_DIR"

if [ ! -f "$KEYS_DIR/private.pem" ]; then
    echo "Generating RSA 2048 private key in $KEYS_DIR/private.pem..."
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$KEYS_DIR/private.pem"
fi

if [ ! -f "$KEYS_DIR/public.pem" ]; then
    echo "Extracting public key in $KEYS_DIR/public.pem..."
    openssl rsa -pubout -in "$KEYS_DIR/private.pem" -out "$KEYS_DIR/public.pem"
fi

echo "Keys successfully verified in $KEYS_DIR/."
