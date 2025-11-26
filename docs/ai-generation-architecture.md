# AI 代码生成系统架构

更新日期：2025-11-25 21:41 NZST · 执行者：Codex

## 系统架构概览
AI 代码生成体系横跨 CLI、核心 `src/ai` 模块以及系统化测试脚本。核心执行路径：用户在 CLI 中运行 `ai-generate` 命令 → `AIGenerator` 聚合 Prompt/Validator/Provider → `GenerationCache` 判定是否命中磁盘 → 未命中时调用 Provider 发起 LLM 请求 → 返回结果后写入缓存、进行政策验证并附加 provenance 元数据。系统化测试脚本以批量并发方式调用 CLI，并通过评估脚本生成 Markdown 报告。

```mermaid
graph TD
  CLI["./dist/scripts/aster.js ai-generate"] --> GEN[AIGenerator]
  GEN --> PM[PromptManager]
  GEN --> VAL[PolicyValidator]
  GEN --> PROV[ProvenanceTracker]
  GEN --> CACHE[GenerationCache]
  GEN --> LLM[(LLMProvider)]
  LLM --> OAI[OpenAIProvider]
  LLM --> ANT[AnthropicProvider]
  CACHE --> FILES[".cache/ai-generation/*.json"]
  TESTS[/run-systematic-tests.mjs/ ] -->|Promise.all 调用| CLI
  EVAL[scripts/evaluate-ai-generation.mjs] --> REPORT[.claude/evaluation-report.md]
```

## 核心组件说明
| 组件 | 位置 | 责任 |
| --- | --- | --- |
| `AIGenerator` | `src/ai/generator.ts` | 管理 end-to-end 工作流：构建 prompt、调用 Provider、执行 `PolicyValidator`、注入 provenance，并负责缓存读写。 |
| `GenerationCache` | `src/ai/generation-cache.ts` | 根据 `${provider}-${model}-temp${temperature}-fs${fewShot}-${hash(description)}` 生成键，将 `GenerateResult` 以 JSON 写入 `.cache/ai-generation/`，记录 hits/misses。 |
| `LLMProvider` & 实现 | `src/ai/llm-provider.ts`、`src/ai/providers/*.ts` | 定义统一 `generate/getName/getModel` 接口，OpenAI/Anthropic 各自封装 API Key、模型参数与错误转换 (`LLMError`)。 |
| `PromptManager` | `src/ai/prompt-manager.ts` | 提供系统提示与 Few-shot 示例，具备内存缓存避免重复 I/O。 |
| `PolicyValidator` | `src/ai/validator.ts` | 对生成代码进行静态校验，输出 diagnostics，CLI 在终端展示结果。 |
| `ProvenanceTracker` | `src/ai/provenance.ts` | 在生成代码头部写入 provider/model/timestamp/validated 状态，便于审计。 |
| `ai-generate` CLI | `src/cli/commands/ai-generate.ts` + `scripts/aster.ts` | 解析选项 (`--provider`, `--model`, `--few-shot-count`, `--temperature`, `--output`, `--no-cache`)，调用 `AIGenerator` 并展示缓存状态。 |
| Systematic Tests | `/tmp/run-systematic-tests.mjs` | 以并发=3 运行 `dev.jsonl` 用例，输出 JSON 结果与实时进度，供 Phase 3.4 性能验证。 |
| 评估脚本 | `scripts/evaluate-ai-generation.mjs` | 解析测试 JSON，生成表格化指标、失败列表与结论，写入 `.claude/evaluation-report.md`。 |

## 缓存机制详解
- **键生成**：`AIGenerator.buildCacheKey()` 组合 provider 名称、模型、温度、Few-shot 数量与描述 SHA-256，避免文件名过长。
- **存储策略**：`GenerationCache.set()` 使用临时文件 + `rename` 原子写入，确保在并发写入时不会得到半成品；目录结构 `.cache/ai-generation/<key>.json` 可直接备份或同步。
- **负载特征**：Phase 3.4 首次运行写入 10 个缓存文件，命中率 62.5%（10/16），第二轮命中后单个用例耗时 <1s。
- **统计**：`getCacheStats()` 暴露 hits/misses/hitRate，CLI 转发 `⚡ 緩存狀態`，系统化脚本可据此评估缓存收益。
- **清理策略**：目前无 TTL，推荐在 CI 中使用 `find .cache/ai-generation -mtime +7 -delete` 按时间清理，或根据项目需求实现 LRU。

## 并发执行机制
Phase 3.4 的 `/tmp/run-systematic-tests.mjs` 使用 Promise 批调度：
```javascript
const CONCURRENCY = 3;
async function runBatch(queue) {
  const slice = queue.splice(0, CONCURRENCY);
  await Promise.all(slice.map(runSingleTest));
}
```
- **任务调度**：根据 `dev.jsonl` 切片构建批次，`Promise.all` 同步等待，保证最短 wall-clock 时间。
- **错误处理**：包裹在 `try/catch` 中，记录 `FAILED` 与 `ERROR` 状态；429 错误直接保留在 JSON `reason` 字段，由评估脚本格式化。
- **进度追踪**：使用 `ora` spinner 打印 `[passed/total]`、`✓/✗/⚠/⚡` 四类统计。
- **Rate limit 控制**：通过限制 `CONCURRENCY=3` 以及第二轮缓存命中机制，将 token 消耗控制在 30k TPM 阈值以内，如遇 429 则需按 Phase 3.4 建议手动重试或降低并发。

## 扩展性设计
1. **添加新 Provider**
   - 创建 `src/ai/providers/<name>.ts`，实现 `LLMProvider` 接口及 `LLMError` 包装。
   - 在 `scripts/aster.ts` 的 `ai-generate` 命令中注册 `--provider <name>`，并在 `createProvider()` 中实例化。
   - 若 Provider 支持自定义参数，可在 CLI `option()` 中暴露额外 flag。
2. **扩展缓存后端**
   - 新建类实现与 `GenerationCache` 相同接口（`get/set/clear/getCacheStats`），在 `AIGenerator` 构造函数注入即可支持例如 Redis/S3。
3. **加入更多 Few-shot 变体**
   - `PromptManager` 读取 `prompts/few-shot-examples.jsonl`，可根据类别或标签选择子集，以 `fewShotCount` 控制混合策略。
4. **评估工作流复用**
   - `scripts/evaluate-ai-generation.mjs` 接口接收任意 JSON 路径，可在 CI 中对不同分支输出多份报告并归档。

## 设计准则
- **解耦**：CLI、核心生成、缓存、测试、评估互相通过清晰接口沟通，便于替换实现。
- **可观测性**：缓存统计、验证结果、Token Usage 均直接暴露，支撑 Phase 3.4 的 81.3% 准确率基线。
- **可扩展性**：Promise 并发与磁盘缓存均基于配置，满足从个人实验到批量评估的不同需求。
