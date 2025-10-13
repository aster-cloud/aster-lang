#!/usr/bin/env bash
set -euo pipefail

NAME=${1:-keycloak-dev}
if podman ps -a --format '{{.Names}}' | grep -q "^${NAME}$"; then
  echo "Stopping and removing ${NAME}..."
  podman rm -f "${NAME}" >/dev/null 2>&1 || true
  echo "Done."
else
  echo "Container ${NAME} not found."
fi

