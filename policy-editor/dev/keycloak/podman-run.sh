#!/usr/bin/env bash
set -euo pipefail

NAME=keycloak-dev
PORT=${PORT:-8083}
IMG=quay.io/keycloak/keycloak:24.0
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if podman ps -a --format '{{.Names}}' | grep -q "^${NAME}$"; then
  echo "Container ${NAME} exists. Stopping and removing..."
  podman rm -f "${NAME}" >/dev/null 2>&1 || true
fi

echo "Starting Keycloak on http://localhost:${PORT} ..."
podman run -d --name "${NAME}" \
  -p "${PORT}:${PORT}" \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -v "${SCRIPT_DIR}/realm-export:/opt/keycloak/data/import:Z" \
  "${IMG}" start-dev --http-port="${PORT}" --import-realm

echo "Done. Users: alice/alice (admin), bob/bob (user)."

