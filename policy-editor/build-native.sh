#!/usr/bin/env bash
set -euo pipefail

# 构建 policy-editor 原生镜像（仅后端能力）
cd ..
./gradlew :policy-editor:clean :policy-editor:build -Dquarkus.package.type=native -x test

echo "Native build finished. Runner binary should be under policy-editor/build." 

