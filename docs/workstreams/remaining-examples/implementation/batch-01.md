# 批次1：验证注解解析修复

- 时间：2025-10-22 01:45 NZST
- 执行者：Codex

## 变更摘要
- 调整 `annotation` 语法规则，允许注解名称既可使用 `IDENT` 也可使用 `TYPE_IDENT`。
- 更新 `AstBuilder` 注解处理，兼容大写注解名称并复用统一提取逻辑。
- 重新生成 ANTLR 语法源码并回归核心/TS 测试，确保既有 108 个样例保持通过。

## 测试结果
- `./gradlew :aster-core:test`
- `npm run test:golden`
- `.claude/scripts/test-all-examples.sh`
  - 执行后通过率：86.2%（113/131，排除预期失败两项）
