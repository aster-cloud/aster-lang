# Phase 4.2 注解一致性与 DTO 验证（P4-2）

**更新日期**: 2025-11-13 20:05 NZST（Codex）  
**阶段状态**: 🟡 进行中

---

## 阶段概览与状态

- P4-2 聚焦在“隐私注解 → AST → DTO/运行时”链路，确保 Phase 4 的 AI 校验器与企业运行时可复用同一套语义。10 月 21 日-11 月 12 日期间共记录 55 条操作，覆盖语法阅读、示例盘点、Shrimp 任务编排以及 Quarkus 回归（见 `docs/workstreams/P4-2/operations-log.md`）。
- 操作轨迹呈现四条主线：① 调研 `@pii`/`@sensitive` 在语法、AST 与 Java 后端中的承载方式；② 完成 test/cnl 示例盘点并识别缺失文件；③ 通过 `.shrimp/tasks.json` 固化 Phase 4.2 子任务与日志绑定；④ 在 `quarkus-policy-api` 上反复生成 Aster JAR 并跑订单工作流回归。
- 交付物当前状态如下：

| 交付物 | 当前状态 | 摘要 | 关键文件 |
| --- | --- | --- | --- |
| 注解语法/AST 差异清单 | 🟡 调研中 | 已逐段审阅 `AsterParser.g4`、`AsterLexer.g4`、`AstBuilder.java`、`Type.java` 与 `JavaCompilerBackend.java`，等待将发现沉淀为变更提案。 | `aster-core/src/main/antlr/AsterParser.g4`、`aster-core/src/main/java/aster/core/parser/AstBuilder.java`、`aster-lang-cli/.../JavaCompilerBackend.java` |
| PII/敏感注解示例与缺口清单 | 🟡 盘点中 | `test/cnl/examples/pii_type_*` 与 `annotations_*` 已复核，`privacy_user_data.aster` 缺失已记录，待补样例与诊断基线。 | `test/cnl/examples/pii_type_basic.aster`、`test/cnl/examples/pii_type_in_data.aster`、`test/cnl/examples/annotations_mixed.aster` |
| Phase 4.2 Shrimp 任务图与日志绑定 | ✅ 已建立 | `.shrimp/tasks.json` 经多次 `apply_patch`/`jq` 校验，结合 `.claude/context-phase4-2.json` 与 operations log 完成任务→证据映射。 | `.shrimp/tasks.json`、`.claude/context-phase4-2.json`、`docs/workstreams/P4-2/operations-log.md` |
| Policy DTO/Quarkus 回归结果 | 🟢 已执行 | `./gradlew :quarkus-policy-api:generateAsterJar`、`:test` 及 `--tests ...OrderWorkflowIntegrationTest` 均已成功执行一次，提供 DTO 命名空间与订单 workflow 的验证快照。 | `aster-asm-emitter/src/main/java/**`、`quarkus-policy-api` 模块 |

---

## 核心目标与成功指标

| 目标 | 成功标准 | 当前进展 |
| --- | --- | --- |
| 注解语法→AST→后端完全贯通 | AsterParser/AstBuilder/JavaCompilerBackend 内新增的注解节点需一一对应，diff 计数为 0。 | 语法/AST/后端已完成人工比对，等待起草差异文档与实现。 |
| PII/敏感样例覆盖 Phase 4 模板 | test/cnl 中 PII/annotations 前缀样例 ≥5，缺失样例有 backlog 记录。 | 已确认 4 个存量样例并记录 privacy_user_data 缺口，后续需扩展真实业务组合。 |
| Shrimp 任务图与日志同步 | `.shrimp/tasks.json` 的每个 P4-2 节点在 operations-log 中至少出现一次。 | 2025-10-22 起的任务维护流程已经形成，后续要在新增操作时持续留痕。 |
| Policy DTO/Quarkus 回归稳定 | `generateAsterJar`、`:test`、`OrderWorkflowIntegrationTest` 均返回成功且无新警告。 | 2025-11-12 的三次 Gradle 命令均成功，下一步把执行结果纳入 CI 报告。 |

---

## 注解语法 / AST / 后端工作面

- 通过 `rg -n '@pii'`、`rg -n '@sensitive'` 以及逐段 `sed` 阅读 `AsterParser.g4`、`AsterLexer.g4`，已经确认词法/语法层面仍以字段注解为主，尚未向类型声明开放。（参见 2025-10-21 23:19 系列命令）
- `AstBuilder.java` 与 `Type.java` 的阅读暴露“注解只停留在 fieldDecl”的现状，需要引入统一的 AnnotationNode 以便后端序列化。（2025-10-21 23:19 | `shell (sed)` 覆盖 1-320 行）
- `JavaCompilerBackend.java` 检查显示 DTO 生成阶段缺少注解串联，现阶段可在 AST → DTO 的转换过程中注入 metadata，确保 Phase 4 的 verifier 与运行时共用一致上下文。

---

## PII/敏感示例与验证策略

- 已对 `test/cnl/examples/pii_type_basic.aster`、`pii_type_in_data.aster` 与 `annotations_mixed.aster` 进行首轮审稿，明确了现有示例集中在“字段标签”与“数据字面量”两类。
- `rg --files -g 'annotations_*.aster'` 表明暂无覆盖“workflow 步骤 + 注解”的组合案例；`privacy_user_data.aster` 缺失说明需要新增贴近实际业务的隐私泄露脚本。
- 下一步需把示例对齐到 Phase 4 的 CNL 模板与 LLM 验证器输入中，并为每个 `.aster` 文件补充诊断基线。

---

## Shrimp 任务图与追踪机制

- 2025-10-22 以 sequential-thinking → `cat .claude/context-phase4-2.json` → `npx mcp-shrimp-task-manager --help` → `apply_patch .shrimp/tasks.json` → `jq` 校验的流程，完成了 Phase 4.2 任务拆分与 UUID/时间戳生成。
- 任务文件与 operations log 采用“一任务一证据”的约束：新增或修改任务后必须在 `docs/workstreams/P4-2/operations-log.md` 追加记录，并同步 `.claude/` 下的分析报告（当前即 `context-p4-2-analysis.json`）。
- 后续应把 shrimp 任务 ID 引入 README/index，方便主 AI 追踪进度并触发验证。

---

## DTO/Policy 回归与风险

- 2025-11-12 的操作完成了 `aster-asm-emitter` 入口定位 → `./gradlew :quarkus-policy-api:generateAsterJar` → `:test` → `--tests ...OrderWorkflowIntegrationTest` 的串联，确保 DTO 生成和关键集成测试通过。
- 初次执行 `:test` 时出现 workflow 死锁并通过复跑解决，提示需要在 CI 中捕获并记录 Gradle 首次失败日志，防止隐患掩盖。
- 建议将本地回归命令写入 `docs/testing.md`，并把订单 workflow 的输出快照与注解差异一起归档。

---

## 风险与下一步

| 风险 | 影响 | 缓解策略 |
| --- | --- | --- |
| 注解语法调研尚未形成提案 | 无法指导 AST/后端同步改造，阻塞 Phase 4 验证器 | 在 `.claude/context-p4-2-analysis.json` 基础上撰写差异文档，列出具体语法/AST 修改点并提交 RFC。 |
| PII 示例覆盖不足 | LLM 输出验证缺乏真实业务案例，难以满足 ROADMAP Phase 4 合规目标 | 补充 workflow + 注解组合场景，并为缺失文件添加 backlog/任务节点。 |
| Shrimp 任务与日志可能脱节 | 难以证明交付痕迹，影响审计 | 采用“新增任务→运行命令→立即记录 ops log”的固定流程，并在 README/index 中引用任务 ID。 |
| Gradle 回归仍为人工触发 | 无法保障多分支并行修改时的稳定性 | 将 `generateAsterJar`/`:test`/`OrderWorkflowIntegrationTest` 纳入 CI，失败时回写 operations log 并附日志链接。 |
