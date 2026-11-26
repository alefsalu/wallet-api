#!/usr/bin/env bash
set -e

git branch --show-current | grep -q "^main$"
grep -q "_PESSOAL/" .gitignore && grep -q "\.env" .gitignore && grep -q "keys/" .gitignore && grep -q "\.pem" .gitignore && grep -q "\.superpowers/" .gitignore
./scripts/guard.sh
echo "Bootstrap test passed."
