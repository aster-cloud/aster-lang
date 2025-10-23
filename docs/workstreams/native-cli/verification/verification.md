# Native CLI 测试记录

- 日期：2025-10-19 23:27 NZDT
- 执行者：Codex
- 指令：
  - `./gradlew :aster-lang-cli:test`
- 结果：
  - 首次执行失败，原因：`settings.gradle` 未包含 `:aster-lang-cli`，以及缺少示例 `test/cnl/examples/hello.aster`。
  - 修正配置并新增示例后重跑通过，验证编译 `hello.aster`、类型检查 `eff_violation_chain.aster` 与版本命令均按预期输出。

- 日期：2025-10-21 20:11 NZST
- 执行者：Codex
- 指令：
  - `./gradlew-java25 :aster-lang-cli:compileJava`
  - `ASTER_COMPILER=java ./gradlew-java25 :aster-lang-cli:run --args 'parse test/cnl/examples/hello.aster --json'`
  - `ASTER_COMPILER=java ./gradlew-java25 :aster-lang-cli:run --args 'parse test/cnl/examples/int_match.aster --json'`
- 结果：
  - Java 编译器后端成功编译。
  - `hello.aster` JSON 输出包含 `Module/Func/Block/Return` 节点及 `span` 信息。
  - `int_match.aster` JSON 输出包含 `Match`、`PatternInt` 与所有分支的 `Return` 节点，序列化字段完整。
