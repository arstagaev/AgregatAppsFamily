#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

PUBLIC_HOST="${PUBLIC_HOST:-10.0.4.180}"
CERT_DIR="./certs"
CERT_FILE="${CERT_DIR}/tls.crt"
KEY_FILE="${CERT_DIR}/tls.key"

mkdir -p "${CERT_DIR}"

if ! command -v openssl >/dev/null 2>&1; then
  echo "[web] ERROR: openssl is not installed"
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "[web] ERROR: docker is not installed"
  exit 1
fi

if [[ ! -s "${CERT_FILE}" || ! -s "${KEY_FILE}" ]]; then
  echo "[web] no TLS certificate found, generating self-signed cert for ${PUBLIC_HOST}"

  if [[ "${PUBLIC_HOST}" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; then
    SAN="IP:${PUBLIC_HOST},IP:127.0.0.1,DNS:localhost"
  else
    SAN="DNS:${PUBLIC_HOST},DNS:localhost,IP:127.0.0.1"
  fi

  openssl req -x509 -nodes -newkey rsa:2048 -sha256 -days 365 \
    -keyout "${KEY_FILE}" \
    -out "${CERT_FILE}" \
    -subj "/CN=${PUBLIC_HOST}" \
    -addext "subjectAltName=${SAN}"
fi

sudo docker compose up -d --force-recreate
sudo docker compose ps
curl -kI https://127.0.0.1:8444/healthz || true
