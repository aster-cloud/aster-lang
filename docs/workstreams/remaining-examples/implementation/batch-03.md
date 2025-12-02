# 批次3：CNL Lambda 与 Result/Option 构造

- 时间：2025-10-22 01:45 NZST
- 执行者：Codex

## 变更摘要
- 调整 `let` 语法，新增 lambda 专用分支，允许 `Let f be function ...` 无需句号结尾。
- 新增 `wrapExpr` 规则并在 `AstBuilder` 中映射 `ok/err/some of` 到 `Expr.Ok/Err/Some`。
- 优化 `postfixExpr`，识别裸 `none` 为 `Expr.None`，并保持 `None()` 等调用兼容。
- 扩展 CLI/test 流程，确认 9 个 CNL Lambda 样例与 Result/Option 构造样例通过。

## 测试结果
- `./gradlew :aster-core:test`
- `npm run test:golden`
- `.claude/scripts/test-all-examples.sh`
  - 执行后通过率：96.1%（126/131，排除预期失败两项）
