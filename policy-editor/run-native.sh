#!/usr/bin/env bash
set -euo pipefail

# 运行 policy-editor 原生可执行文件
BIN=$(ls -1 build/*-runner 2>/dev/null | head -n1 || true)
if [[ -z "${BIN}" ]]; then
  echo "未找到原生可执行文件，请先执行 build-native.sh"
  exit 1
fi

echo "启动 ${BIN} ..."
exec "${BIN}" -Dquarkus.http.host=0.0.0.0

