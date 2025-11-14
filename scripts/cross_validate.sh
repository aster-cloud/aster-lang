#!/bin/bash
# 端到端注解集成测试入口：构建 → TypeScript E2E → 跨栈诊断对比。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TS_CLI="$ROOT/dist/scripts/typecheck-cli.js"
JAVA_CLI="$ROOT/aster-core/build/install/aster-core/bin/aster-core"
DIAG_DIFF="${DIAG_DIFF_CMD:-node --loader ts-node/esm \"$ROOT/tools/diagnostic_diff.ts\" --ignore-span}"

normalize_diags() {
  local input_path="$1"
  local output_path="$2"
  node - <<'EOF' "$input_path" "$output_path"
import fs from 'node:fs';

const [inputPath, outputPath] = process.argv.slice(2);
const allowed = new Set(['E200', 'E302', 'E303']);
const raw = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
const diags = Array.isArray(raw.diagnostics) ? raw.diagnostics : [];
const normalized = {
  diagnostics: diags
    .filter(diag => allowed.has(String(diag.code ?? '').toUpperCase()))
    .map(diag => ({
      code: String(diag.code ?? '').toUpperCase(),
      severity: String(diag.severity ?? '').toLowerCase(),
    })),
};
fs.writeFileSync(outputPath, JSON.stringify(normalized, null, 2), 'utf8');
EOF
}

echo "=== 端到端集成测试 ==="

echo "⚙️  编译 TypeScript 工具链"
(cd "$ROOT" && npm run build)

echo "⚙️  编译 Java 组件"
(cd "$ROOT" && ./gradlew :aster-core:installDist :aster-asm-emitter:build)

echo "=== TypeScript E2E Tests ==="
(cd "$ROOT" && node --test dist/test/e2e/annotation-integration.test.js)

echo "=== Cross-stack Diagnostic Validation ==="
shopt -s nullglob
files=("$ROOT"/test/e2e/*.aster)
if [[ ${#files[@]} -eq 0 ]]; then
  echo "未找到 test/e2e/*.aster 测试样例" >&2
  exit 1
fi

if [[ ! -f "$TS_CLI" ]]; then
  echo "缺少 $TS_CLI，请先运行 npm run build" >&2
  exit 1
fi
if [[ ! -x "$JAVA_CLI" ]]; then
  echo "缺少 Java CLI，可通过 ./gradlew :aster-core:installDist 构建" >&2
  exit 1
fi

for file in "${files[@]}"; do
  base="$(basename "$file")"
  echo "--- 比对 $base ---"
  ts_tmp="$(mktemp)"
  java_tmp="$(mktemp)"
  ts_norm="$(mktemp)"
  java_norm="$(mktemp)"
  if ! (cd "$ROOT" && node "$TS_CLI" "$file" > "$ts_tmp"); then
    echo "TypeScript 诊断生成失败: $base" >&2
    rm -f "$ts_tmp" "$java_tmp"
    exit 1
  fi
  if ! (cd "$ROOT" && "$JAVA_CLI" typecheck "$file" > "$java_tmp"); then
    echo "Java 诊断生成失败: $base" >&2
    rm -f "$ts_tmp" "$java_tmp"
    exit 1
  fi
  normalize_diags "$java_tmp" "$java_norm"
  normalize_diags "$ts_tmp" "$ts_norm"

  if ! eval "$DIAG_DIFF \"$java_norm\" \"$ts_norm\""; then
    echo "诊断输出不一致: $base" >&2
    rm -f "$ts_tmp" "$java_tmp"
    exit 1
  fi
  rm -f "$ts_tmp" "$java_tmp" "$ts_norm" "$java_norm"
done

echo "✅ 所有端到端集成测试通过"
