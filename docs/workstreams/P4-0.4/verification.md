# P4-0.4 验证记录
- 日期: 2025-11-14 07:49 NZST (Codex)

## AST 模式
1. `node --loader ts-node/esm tools/ast_diff.ts test/fixtures/ast-output-ts.json test/fixtures/ast-output-ts.json`
   - 结果: ✅ 输出“AST一致，两侧输出完全匹配。”
2. `node --loader ts-node/esm tools/ast_diff.ts test/fixtures/ast-output-ts.json test/fixtures/ast-output-java.json`
   - 结果: ❌ 输出 3 条差异，包含路径与左右值颜色高亮。

## 诊断模式 (通过 ast_diff)
3. `node --loader ts-node/esm tools/ast_diff.ts --mode=diagnostics test/fixtures/diag-output-ts.json test/fixtures/diag-output-ts.json`
   - 结果: ✅ 输出“诊断一致”。
4. `node --loader ts-node/esm tools/ast_diff.ts --mode=diagnostics test/fixtures/diag-output-ts.json test/fixtures/diag-output-java.json`
   - 结果: ❌ 输出 code/severity/message/help 差异及修复建议。

## 诊断专用脚本
5. `node --loader ts-node/esm tools/diagnostic_diff.ts test/fixtures/diag-output-ts.json test/fixtures/diag-output-ts.json`
   - 结果: ✅ 输出“诊断一致”。
6. `node --loader ts-node/esm tools/diagnostic_diff.ts test/fixtures/diag-output-ts.json test/fixtures/diag-output-java.json`
   - 结果: ❌ 输出同样的高亮差异与建议，exit code = 1。

## 说明
- 所有命令均在项目根目录执行。
- 诊断样例使用 `test/fixtures/diag-output-*.json`，AST 样例使用 `test/fixtures/ast-output-*.json`。
- 失败命令均产生 exit code 1 以便 CI 捕获。
