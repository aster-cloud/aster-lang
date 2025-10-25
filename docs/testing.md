# 测试执行记录

## 2025-10-08 结构化日志系统联调
- 日期：2025-10-08 14:50 NZST
- 执行者：Codex
- 指令与结果：
  - `npm run typecheck` → 通过（tsc --noEmit）。
  - `npm run test` → 通过（黄金测试、属性测试全部成功，输出结构化 JSON 日志）。
  - `LOG_LEVEL=DEBUG node dist/scripts/typecheck-cli.js test/cnl/examples/id_generic.aster` → 通过，输出 INFO 级日志与性能指标。
  - `ASTER_DEBUG_TYPES=1 LOG_LEVEL=DEBUG node dist/scripts/typecheck-cli.js test/cnl/examples/id_generic.aster` → 通过，输出与上次一致。

## 2025-10-08 Typecheck 能力验证
- 日期：2025-10-08 16:33 NZDT
- 执行者：Codex
- 指令与结果：
  - `npm run build` → 通过（tsc 完成编译并生成 PEG 解析器）。
  - `npm run typecheck` → 通过（tsc --noEmit 确认类型检查无误）。

## 2025-10-08 黄金测试细粒度能力更新
- 日期：2025-10-08 16:45 NZDT
- 执行者：Codex
- 指令与结果：
  - `ASTER_CAP_EFFECTS_ENFORCE=1 npm run test:golden` → 通过，所有 eff_violation/eff_caps_enforce/pii 黄金测试均输出细粒度 capability 文案，其余 AST/Core 黄金测试保持成功。

## 2025-10-08 Capability v2 收尾验证
- 日期：2025-10-08 16:56 NZDT
- 执行者：Codex
- 指令与结果：
  - `npm run typecheck` → 通过（tsc --noEmit，确认 TypeScript 侧无回归）。
  - `npm run test:golden` → 通过（黄金测试与格式化流程完整执行）。
  - `npm run build` → 通过（生成 PEG 解析器）。
  - `node dist/scripts/typecheck-cli.js test/capability-v2.aster` → 通过但提示 `mixed` 无直接 IO 操作；用于验证 legacy `@io` 与细粒度 `Http`/`Files`/`Secrets` 注解可被解析。

## 2025-10-15 P0 缓存修复验证
- 日期：2025-10-15 19:21 NZST
- 执行者：Codex
- 指令与结果：
  - `./gradlew :quarkus-policy-api:test` → 失败（缺少 `test/cnl/stdlib/finance/loan.cnl` 等策略资产，任务 `generateAsterJar` 退出码 1）

## 2025-10-17 quarkus-policy-api 测试回归
- 日期：2025-10-17 09:32 NZDT
- 执行者：Codex
- 指令与结果：
  - `./gradlew :quarkus-policy-api:test` → 通过（生成策略类并运行全部测试，无编译错误）

## 2025-10-19 Native CLI 集成测试
- 日期：2025-10-19 23:27 NZDT
- 执行者：Codex
- 指令与结果：
  - `./gradlew :aster-lang-cli:test` → 首次因模块未在 settings.gradle 中注册而失败，修复配置与样例后重跑通过（生成 JAR、编译 hello.aster、完成 CLI 单元/集成测试）

## 2025-10-21 AST 序列化验证
- 日期：2025-10-21 20:11 NZST
- 执行者：Codex
- 指令与结果：
  - `./gradlew-java25 :aster-lang-cli:compileJava` → 通过（确认 Java 编译器后端增量代码可编译）
  - `ASTER_COMPILER=java ./gradlew-java25 :aster-lang-cli:run --args 'parse test/cnl/examples/hello.aster --json'` → 通过（输出包含 `Module/Func/String` 等节点完整 JSON）
  - `ASTER_COMPILER=java ./gradlew-java25 :aster-lang-cli:run --args 'parse test/cnl/examples/int_match.aster --json'` → 通过（输出 `Match` 与 `PatternInt` 节点 JSON）

## 2025-10-21 P4 批次 2 类型注解
- 日期：2025-10-21 23:40 NZST
- 执行者：Codex
- 指令与结果：
  - `./gradlew-java25 :aster-core:test` → 首次因 `Decl.TypeAlias` 名称解析空指针失败，修复后重跑通过。
  - `./gradlew-java25 :aster-core:test` → 通过（174 个测试，新增类型别名与注解用例通过）。
  - `./.claude/scripts/test-all-examples.sh` → 通过脚本执行，48/131 成功（36.6%）；批次示例仍有注解与比较符相关语法未覆盖。

## 2025-10-22 Phase 5.3 回归测试修复
- 日期：2025-10-22 22:05 NZST
- 执行者：Codex
- 指令与结果：
  - `npm run build` → 通过（编译 dist 并生成 PEG 解析器）。
  - `npm run test:regression` → 通过（6/6 通过，4 个 TODO 用例已注释跳过）。

## 2025-10-24 TypeSystem.equals 测试扩展验证
- 日期：2025-10-24 13:21 NZST
- 执行者：Codex
- 指令与结果：
  - `npm test` → 通过（串行执行 fmt、build、unit、integration、golden、property 流水线，全量用例成功）。
  - `npm run test:coverage` → 通过（生成覆盖率报告，`src/typecheck/type_system.ts` equals 分支命中）。

## 2025-10-24 TypeSystem helper 覆盖率提升
- 日期：2025-10-24 14:00 NZST
- 执行者：Codex
- 指令与结果：
  - `npm run test:unit` → 首次因 Core.Parameter 缺少 annotations 报错，修复测试数据后重跑通过。
  - `npm run test:coverage` → 通过（`src/typecheck/type_system.ts` statements 覆盖率提升至 76.09%，format/expand/infer/ConstraintSolver 分支命中）。

## 2025-10-25 Native 构建阶段 E 综合验证
- 日期：2025-10-25 17:34 NZST
- 执行者：Codex
- 指令与结果：
  - `ASTER_COMPILER=java ./gradlew :aster-lang-cli:test` → 通过（生成 CLI JAR，执行全部单元与集成测试）。
  - `./gradlew build` → 失败（`test/cnl/stdlib/finance/loan.aster` 缺失导致 `:quarkus-policy-api:generateAsterJar` 与 `:aster-lang-cli:generateAsterJar` 退出码 1）。
  - `./gradlew :aster-lang-cli:run --args="--help"` → 通过（帮助文本包含 `native` 命令及相关选项）。
  - `ASTER_COMPILER=java ./gradlew :aster-lang-cli:test` → 通过（验证 Java 编译器后端回归）。
  - `./gradlew :aster-lang-cli:test` → 通过（默认 TypeScript 编译器后端测试通过）。
