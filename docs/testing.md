# 测试执行记录

## 2025-10-08 结构化日志系统联调
- 日期：2025-10-08 14:50 NZST
- 执行者：Codex
- 指令与结果：
  - `npm run typecheck` → 通过（tsc --noEmit）。
  - `npm run test` → 通过（黄金测试、属性测试全部成功，输出结构化 JSON 日志）。
  - `LOG_LEVEL=DEBUG node dist/scripts/typecheck-cli.js cnl/examples/id_generic.aster` → 通过，输出 INFO 级日志与性能指标。
  - `ASTER_DEBUG_TYPES=1 LOG_LEVEL=DEBUG node dist/scripts/typecheck-cli.js cnl/examples/id_generic.aster` → 通过，输出与上次一致。

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
  - `./gradlew :quarkus-policy-api:test` → 失败（缺少 `cnl/stdlib/finance/loan.cnl` 等策略资产，任务 `generateAsterJar` 退出码 1）
