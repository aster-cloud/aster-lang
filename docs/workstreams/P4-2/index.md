# P4-2: 注解一致性与 DTO 验证索引

**更新日期**: 2025-11-13 20:05 NZST（Codex）  
**状态**: 🟡 进行中  
**测试通过率**: 3/3（最近一次 `generateAsterJar` / `:test` / `OrderWorkflowIntegrationTest` 全部通过）

---

## 概述与关键指标

P4-2 要求隐私注解在 DSL → AST → DTO → Quarkus 运行时全链路一致，并以 Shrimp 任务与 operations log 记录执行证据。当前侧重语法/AST 调研、PII 示例盘点、任务编排以及运行时回归。

| 指标 | 当前值 | 目标值 | 数据来源 |
| --- | --- | --- | --- |
| 注解差异清单完成度 | “语法/AST/后端”三处人工对比完成，尚未提交变更 | 提交 RFC + patch，diff=0 | `aster-core/src/main/antlr/AsterParser.g4`、`aster-core/src/main/java/aster/core/parser/AstBuilder.java`、`aster-lang-cli/src/main/java/aster/cli/compiler/JavaCompilerBackend.java` |
| PII/敏感示例覆盖率 | 4 个现有样例 + 1 个缺口（privacy_user_data） | ≥5 个样例 + 100% 缺口登记 | `test/cnl/examples/pii_type_basic.aster`、`pii_type_in_data.aster`、`annotations_mixed.aster`、operations log |
| Shrimp 任务/日志对齐度 | `.shrimp/tasks.json` 中的 Phase 4.2 节点均已在日志出现一次 | 新增节点 24h 内必须在 ops log 留痕 | `.shrimp/tasks.json`、`docs/workstreams/P4-2/operations-log.md` |
| Policy DTO 回归稳定度 | 最近一次 `generateAsterJar` + `:test` + `OrderWorkflowIntegrationTest` 全部成功 | 每次注解相关改动需重跑三条命令 | `quarkus-policy-api/build.gradle`、gradle 输出 |

---

## 快速导航

- [README（阶段概览）](./README.md)
- [Operations Log（证据表）](./operations-log.md)
- [.claude/context-p4-2-analysis.json](../../.claude/context-p4-2-analysis.json)
- [Shrimp 任务列表](../../.shrimp/tasks.json)
- [注解/语法入口](../../aster-core/src/main/antlr)
- [AST/后端实现](../../aster-core/src/main/java/aster/core)
- [PII 示例目录](../../test/cnl/examples)
- [Quarkus policy API 模块](../../quarkus-policy-api)

---

## 核心改动与参考代码

1. **语法与 AST 巡检**：
   - `AsterParser.g4` / `AsterLexer.g4`：确认 `annotation` 目前只绑定在 `fieldDecl`，为后续扩展 typeDecl 提供依据。
   - `AstBuilder.java` / `Type.java`：梳理 AnnotationNode 装配点，定位需要传递到 Type metadata 的字段。
2. **后端/DTO 出口**：`aster-lang-cli/src/main/java/aster/cli/compiler/JavaCompilerBackend.java` 用于检查注解是否被写入 DTO。结合 `aster-asm-emitter` 可验证字节码生成是否带上 metadata。
3. **PII 示例**：`test/cnl/examples/pii_type_basic.aster`、`pii_type_in_data.aster`、`annotations_mixed.aster` 提供实际脚本，可复制到新的黄金用例。
4. **任务追踪**：`.shrimp/tasks.json` + `docs/workstreams/P4-2/operations-log.md` 构成“任务→证据”链路，任何新步骤均需在两处更新。

```bash
# 检查注解在语法中的出现位置
rg -n "@pii" -n "@sensitive" aster-core/src/main/antlr

# 运行 Shrimp 任务工具（需 Node 18+）
npx -y mcp-shrimp-task-manager --help

# 回归 Policy DTO + 工作流
./gradlew :quarkus-policy-api:generateAsterJar
./gradlew :quarkus-policy-api:test
./gradlew :quarkus-policy-api:test --tests io.aster.ecommerce.integration.OrderWorkflowIntegrationTest
```

---

## 成功标准与监控

| 成功标准 | 监控方式 | 触发动作 |
| --- | --- | --- |
| 注解语法→AST→后端全贯通 | diff AsterParser vs AstBuilder vs JavaCompilerBackend；任何缺失立即登记 | 在 `.claude/context-p4-2-analysis.json` 更新差异并创建任务 |
| PII/敏感样例 ≥5 且缺口归档 | 统计 `test/cnl/examples` 中 `pii_*`、`annotations_*` 文件；缺失写入 `.shrimp/tasks.json` | 每次新增样例后生成配套诊断基线 |
| Shrimp 任务 100% 留痕 | 对比 `.shrimp/tasks.json` 与 `operations-log.md`，若任务未出现则阻断合并 | 更新 ops log 同时打上任务 ID |
| Policy 回归命令全绿 | 每次注解/DTO改动后运行三条 gradle 命令并上传输出 | 失败则在 ops log 记录日志片段并开启修复任务 |

---

## 验证与测试记录

- `2025-11-12 23:11 NZST`：`./gradlew :quarkus-policy-api:generateAsterJar`，验证新的命名空间映射。（见 operations log）
- `2025-11-12 23:22 NZST`：`./gradlew :quarkus-policy-api:test`，全量测试复跑通过，首次死锁日志需归档。
- `2025-11-12 23:26 NZST`：`./gradlew :quarkus-policy-api:test --tests io.aster.ecommerce.integration.OrderWorkflowIntegrationTest`，订单工作流回归成功。
- 后续任务：将以上命令写入 CI 并把输出链接到 README/index。

---

## 依赖与部署清单

1. **工具链**：Node.js 18+（安装 `mcp-shrimp-task-manager`）、npm、rg、sed、Python 3.11（生成 UUID/时间戳）。
2. **Java/构建**：GraalVM or Java 17 + Gradle，用于 `aster-asm-emitter` 与 `quarkus-policy-api` 相关任务。
3. **文档/分析**：`.claude/context-p4-2-analysis.json`、`.claude/context-p4-0-analysis.json` 作为模板与审计依据。
4. **流程要求**：新增任务需同步 `.shrimp/tasks.json` 与 `docs/workstreams/P4-2/operations-log.md`；任何回归命令必须记录输出摘要与时间戳。
