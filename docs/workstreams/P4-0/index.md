# P4-0: 类型/效果验证基线索引

**更新日期**: 2025-11-13 19:39 NZST（Codex）  
**状态**: 🟡 进行中  
**测试通过率**: —（等待 cross_validate 汇报）

---

## 概述与状态

P4-0 聚焦于把 Phase 3 提供的类型/效果/PII/LSP 能力沉淀为可复制的黄金用例与统一错误码体系，再通过跨语言 diff 工具保证 Java 与 TypeScript 诊断输出对齐。目前错误码与场景文档已落地，黄金用例与 cross_validate 仍在补完阶段。

### 关键指标仪表盘

| 指标 | 当前值 | 目标值 | 数据来源 |
| --- | --- | --- | --- |
| 错误码映射覆盖率 | 100%（`scripts/generate_error_codes.ts` 成功写出 TS/Java 枚举） | 100%（禁止孤立诊断） | `shared/error_codes.json`、`src/error_codes.ts` |
| 黄金用例分类 | 16 个 `.aster` 用例，覆盖 4 类场景 + 4 个工作流案例 | 每个类别 ≥ 2 个高信噪样例，累计 ≥ 10 个 | `test/type-checker/golden/`、`test/type-checker/scenarios/**/README.md` |
| 跨语言 diff | 未执行（脚本已就绪，尚未挂 CI） | `scripts/cross_validate.sh` 返回码 0，diff 计数 0 | `tools/ast_diff.ts`、`scripts/cross_validate.sh` |
| 场景文档可复用度 | 4 份 README 已描述触发条件/诊断/扩展指南 | 每份场景文档包含 3 要素 | `test/type-checker/README.md`、`test/type-checker/scenarios/**/README.md` |

---

## 快速导航

- [README（阶段概览）](./README.md)
- [Operations Log（详细操作记录）](./operations-log.md)
- [黄金用例源文件](../../test/type-checker/golden)
- [预期输出基线](../../test/type-checker/expected)
- [场景说明文档](../../test/type-checker/scenarios)
- [错误码生成脚本](../../scripts/generate_error_codes.ts)
- [AST Diff 工具与交叉验证脚本](../../tools/ast_diff.ts)、[scripts/cross_validate.sh](../../scripts/cross_validate.sh)

---

## 核心改动与代码示例

1. **统一错误码注册表**：`shared/error_codes.json` → `scripts/generate_error_codes.ts` → `src/error_codes.ts` 与 `aster-core/.../ErrorCode.java`，确保 Java/TS 共用编号、分类与消息模板。
2. **黄金用例 + 预期输出**：`test/type-checker/golden/*.aster` 与 `expected/*.json`/`.errors.json` 固定类型推断与诊断，高信噪案例覆盖类型、效果、PII、能力与工作流补偿。
3. **AST Diff & Cross-Validate**：`tools/ast_diff.ts` 归一化 JSON 并输出差异，`scripts/cross_validate.sh` 执行两端类型检查并驱动 diff，准备接入 Phase 4 验证流水线。

```bash
# 生成/更新错误码常量
node --loader ts-node/esm scripts/generate_error_codes.ts

# 在 TypeScript 诊断中格式化消息
import { ErrorCode, formatErrorMessage } from '../src/error_codes';

const msg = formatErrorMessage(ErrorCode.TYPE_MISMATCH, {
  expected: 'Int',
  actual: 'String',
});
```

---

## 监控与成功标准

| 成功标准 | 验证方法 | 责任 artefact |
| --- | --- | --- |
| 错误码映射 100% 覆盖 | 运行生成脚本后对比 `shared/error_codes.json` 与 `src/error_codes.ts`/`ErrorCode.java` 的枚举数量；任何 diff 需在 `operations-log` 记录。 | `shared/error_codes.json`、`scripts/generate_error_codes.ts` |
| 黄金用例分类完备 | 统计 `test/type-checker/golden/*.aster` 并核对 `scenarios/**/README.md` 表格；确保每个类别至少 2 个并在 `expected/` 中存在 `<case>.json`/`.errors.json`。 | `test/type-checker/golden`、`test/type-checker/expected` |
| 跨语言差异为零 | 设置 `JAVA_TYPECHECK_CMD`/`TS_TYPECHECK_CMD` 后运行 `scripts/cross_validate.sh`，确认 `tools/ast_diff.ts` 未输出差异且脚本退出码为 0。 | `tools/ast_diff.ts`、`scripts/cross_validate.sh` |
| 场景文档可供复用 | 审查 `test/type-checker/README.md` 以及四个场景目录，确保均记录触发条件、期望诊断与扩展指南；缺失要素视为未达标。 | `test/type-checker/README.md`、`test/type-checker/scenarios/**/README.md` |

---

## 测试覆盖与验证结果

- **覆盖范围**：16 个黄金用例 + 32 个预期输出，涵盖类型推断稳定性、效果声明、Await 纪律、PII HTTP 违规、Secrets 能力缺失以及 4 个工作流补偿场景。
- **验证数据**：`expected/*.json` 记录函数签名、效果/能力数组；`.errors.json` 固定 `code`、`message`、`span`，为 cross_validate 与 CI diff 提供基线。
- **执行状态**：`scripts/cross_validate.sh` 已可本地运行，但尚无 recorded run；需要在 CI 中定期拉起并把 diff 结果回写到 `docs/workstreams/P4-0/operations-log.md`。

---

## 部署准备清单

1. **工具链准备**：安装 Node.js 18+、`ts-node/esm`、`rg`/`sed`，并确保 `JAVA_TYPECHECK_CMD`、`TS_TYPECHECK_CMD` 指向 Phase 3 产出的 Java/TS 类型检查可执行体。
2. **依赖 Phase 3 能力**：复用 Phase 3 已交付的类型/效果检查实现、LSP PII 诊断模块与 capability packs（HTTP/SQL/Vault），避免重复建设。
3. **脚本集成**：在 CI 中串联 `scripts/generate_error_codes.ts`、`scripts/cross_validate.sh`、`tools/ast_diff.ts`，将失败视为阻断。
4. **文档签审**: 对 `test/type-checker/README.md` 与各场景 README 进行 peer review，并将审批结果记录在 `docs/workstreams/P4-0/operations-log.md`。
5. **数据更新流程**：确立新增黄金用例 → 更新 expected → 运行 cross_validate → 在 operations log 留痕的流程，防止无基线的提交进入主干。
