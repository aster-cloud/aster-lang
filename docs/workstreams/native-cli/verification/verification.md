# Native CLI 测试记录

- 日期：2025-10-19 23:27 NZDT
- 执行者：Codex
- 指令：
  - `./gradlew :aster-lang-cli:test`
- 结果：
  - 首次执行失败，原因：`settings.gradle` 未包含 `:aster-lang-cli`，以及缺少示例 `cnl/examples/hello.aster`。
  - 修正配置并新增示例后重跑通过，验证编译 `hello.aster`、类型检查 `eff_violation_chain.aster` 与版本命令均按预期输出。
