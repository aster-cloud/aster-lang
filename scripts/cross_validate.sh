#!/bin/bash
# 对比 Java 与 TypeScript 类型检查器输出，确保结果一致。
set -u -o pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GOLDEN_DIR="$ROOT/test/type-checker/golden"

JAVA_CMD="${JAVA_TYPECHECK_CMD:-}"
TS_CMD="${TS_TYPECHECK_CMD:-}"
AST_DIFF_CMD="${AST_DIFF_CMD:-node --loader ts-node/esm \"$ROOT/tools/ast_diff.ts\"}"

if [[ -z "$JAVA_CMD" || -z "$TS_CMD" ]]; then
  echo "请设置 JAVA_TYPECHECK_CMD 与 TS_TYPECHECK_CMD 环境变量后再运行。" >&2
  exit 2
fi

if ! compgen -G "$GOLDEN_DIR/*.aster" > /dev/null; then
  echo "未找到任何 golden 测试用例，路径：$GOLDEN_DIR" >&2
  exit 1
fi

overall_status=0

for file in "$GOLDEN_DIR"/*.aster; do
  base="$(basename "$file")"
  echo "=== 比对 $base ==="

  java_tmp="$(mktemp)"
  ts_tmp="$(mktemp)"

  if ! eval "$JAVA_CMD \"$file\"" > "$java_tmp"; then
    echo "Java 类型检查失败: $file" >&2
    overall_status=1
    rm -f "$java_tmp" "$ts_tmp"
    continue
  fi

  if ! eval "$TS_CMD \"$file\"" > "$ts_tmp"; then
    echo "TypeScript 类型检查失败: $file" >&2
    overall_status=1
    rm -f "$java_tmp" "$ts_tmp"
    continue
  fi

  if ! eval "$AST_DIFF_CMD \"$java_tmp\" \"$ts_tmp\""; then
    echo "AST 输出不一致: $file" >&2
    overall_status=1
  fi

  rm -f "$java_tmp" "$ts_tmp"
done

if [[ $overall_status -eq 0 ]]; then
  echo "🎉 所有测试的 Java 与 TS 类型检查结果一致。"
else
  echo "⚠️ 交叉验证存在差异，请修复后重试。"
fi

exit $overall_status
