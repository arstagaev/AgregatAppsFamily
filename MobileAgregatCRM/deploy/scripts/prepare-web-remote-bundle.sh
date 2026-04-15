#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

OUT_DIR="${1:-${ROOT_DIR}/deploy/out/web-remote-package}"
SKIP_BUILD="${SKIP_BUILD:-0}"

DIST_DIR="${ROOT_DIR}/composeApp/build/dist/wasmJs/productionExecutable"

echo "[prepare-web] root: ${ROOT_DIR}"
echo "[prepare-web] out : ${OUT_DIR}"

if [[ "${SKIP_BUILD}" != "1" ]]; then
  echo "[prepare-web] building wasm production bundle..."
  (cd "${ROOT_DIR}" && ./gradlew :composeApp:wasmJsBrowserDistribution -q)
else
  echo "[prepare-web] SKIP_BUILD=1, using existing artifacts"
fi

if [[ ! -f "${DIST_DIR}/index.html" ]]; then
  echo "[prepare-web] ERROR: missing ${DIST_DIR}/index.html"
  echo "[prepare-web] run build first: ./gradlew :composeApp:wasmJsBrowserDistribution"
  exit 1
fi

rm -rf "${OUT_DIR}"
mkdir -p "${OUT_DIR}/web-dist" "${OUT_DIR}/certs"

cp "${ROOT_DIR}/deploy/docker/docker-compose.yml" "${OUT_DIR}/docker-compose.yml"
cp "${ROOT_DIR}/deploy/docker/nginx/mobileagregatcrm-web-docker.conf" "${OUT_DIR}/mobileagregatcrm-web-docker.conf"

if command -v rsync >/dev/null 2>&1; then
  rsync -a --delete "${DIST_DIR}/" "${OUT_DIR}/web-dist/"
else
  cp -R "${DIST_DIR}/." "${OUT_DIR}/web-dist/"
fi

cp "${ROOT_DIR}/composeApp/src/wasmJsMain/resources/endpoint-health.html" "${OUT_DIR}/web-dist/endpoint-health.html"

# Keep bundle deterministic and clean for Linux servers.
find "${OUT_DIR}" -name ".DS_Store" -type f -delete

cat > "${OUT_DIR}/run-on-server.sh" <<'EOF'
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
EOF
chmod +x "${OUT_DIR}/run-on-server.sh"

cat > "${OUT_DIR}/README.txt" <<'EOF'
Upload this whole folder to your remote server (for example: ~/trrapp).

Required files here:
- docker-compose.yml
- mobileagregatcrm-web-docker.conf
- web-dist/
- certs/ (can be empty; cert is auto-generated on first launch)

Launch web container:
  bash run-on-server.sh

Open in browser:
  https://10.0.4.180:8444/

Notes:
- This bundle is standalone (no host nginx config required).
- First run generates a self-signed TLS cert in ./certs.
- Browser will show certificate warning until you trust this cert.

Health check:
  curl -kI https://127.0.0.1:8444/healthz
EOF

echo "[prepare-web] bundle created:"
echo "  ${OUT_DIR}"
echo "[prepare-web] next:"
echo "  1) upload this folder to server"
echo "  2) run: bash run-on-server.sh"
