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

- 日期：2025-11-25 09:17 NZDT
- 执行者：Codex
- 指令：
  - `npm run build`
  - `npm run test:cli`
  - `npm run test:cli:coverage`
  - `npm run test:e2e:cli`
- 结果：
  - build：完成 CLI/测试脚本编译，确保 dist/scripts 与 dist/test/* 均为最新产物。
  - test:cli：12 项 CLI 单元/辅助测试全部通过，覆盖 install/list/error-handler 正常路径与错误分支。
  - test:cli:coverage：通过 `c8 --include 'dist/src/cli/**/*.js'` 统计，语句 86.71%、分支 75.18%、函数 94.11%、行 86.71%。
  - test:e2e:cli：在临时工作区及 `.aster/local-registry` 下使用真实 `./dist/scripts/aster.js` 成功执行 install + list 流程。
