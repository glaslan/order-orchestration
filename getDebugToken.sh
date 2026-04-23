#!/usr/bin/env bash
set -euo pipefail

secret='farm2fork-orders-dev-secret-key!'

base64url() {
  openssl base64 -e -A | tr '+/' '-_' | tr -d '='
}

header='{"alg":"HS256","typ":"JWT"}'
payload='{"id":"test-user","name":"Tester Testington","email":"dev@duck.com","phone":"666-6767","role":"customer"}'

header_b64=$(printf '%s' "$header" | base64url)
payload_b64=$(printf '%s' "$payload" | base64url)

sig=$(printf '%s' "$header_b64.$payload_b64" \
  | openssl dgst -sha256 -hmac "$secret" -binary \
  | base64url)

jwt="$header_b64.$payload_b64.$sig"
printf '%s\n' "$jwt"
