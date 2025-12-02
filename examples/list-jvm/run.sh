#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || cd "$SCRIPT_DIR/../.." && pwd)"

cd "$ROOT_DIR"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT_DIR/build/.gradle}" ./gradlew :examples:list-jvm:run

