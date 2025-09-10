#!/usr/bin/env bash
set -euo pipefail

# 1) Emit classfiles for greet
npm run emit:class cnl/examples/greet.cnl

# 2) Package to jar
npm run jar:jvm

# 3) Build native image (requires GraalVM setup)
./gradlew :examples:hello-native:nativeCompile

echo "Binary at examples/hello-native/build/native/nativeCompile/hello-aster"

