#!/usr/bin/env bash
set -euo pipefail

if git ls-files | grep -E '^(_PESSOAL/|\.superpowers/|keys/)|\.pem$|(^|/)\.env$'; then
    echo "ERROR: Protected files tracked in git!" && exit 1
fi

echo "Guard check passed: no protected files tracked."
