#!/usr/bin/env bash
set -euo pipefail

OUTPUT_FILE="${1:-openapi/openapi.yaml}"
mkdir -p "$(dirname "$OUTPUT_FILE")"

URL="${OPENAPI_URL:-http://localhost:8080/v3/api-docs.yaml}"
echo "Fetching OpenAPI spec from $URL..."
curl -sSf "$URL" -o "$OUTPUT_FILE"
echo "Exported OpenAPI spec to $OUTPUT_FILE"
