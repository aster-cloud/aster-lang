# 批次2：Map 类型语法扩展

- 时间：2025-10-22 01:45 NZST
- 执行者：Codex

## 变更摘要
- 新增 `MAP` 词法 token，并扩展类型文法以支持 `Map Text to Text` 形式。
- 为表达式解析增加 `Map` 识别分支，保证 `Map.get` 等调用正常工作。
- 在 `AstBuilder` 引入 `visitMapType`，构造 `Type.Map` AST 节点。

## 测试结果
- `./gradlew :aster-core:test`
- `npm run test:golden`
- `.claude/scripts/test-all-examples.sh`
  - 执行后通过率：87.0%（114/131）
