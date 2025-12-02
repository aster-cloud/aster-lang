# AI 代码生成用户指南

更新日期：2025-11-25 21:41 NZST · 执行者：Codex

## 概述
AI 代码生成流程由 `./dist/scripts/aster.js ai-generate` 命令驱动，通过 Prompt Manager + Few-shot 示例构建出完整提示，再调用 `AIGenerator` 协调 LLM Provider、验证器与 Provenance 追踪器。Phase 3.4 已实测达到 81.3% 准确率基线（13/16，用例集来源 `test/ai-generation/dev.jsonl`），并通过磁盘缓存与并发执行将批量生成耗时降至 <3 分钟。

## 快速开始
1. **安装依赖**
   ```bash
   npm install
   npm run build
   ```
2. **配置 API 密钥**（根据 provider 选择）
   ```bash
   export OPENAI_API_KEY="sk-..."
   # 或
   export ANTHROPIC_API_KEY="sk-ant-..."
   ```
3. **运行生成命令**
   ```bash
   ./dist/scripts/aster.js ai-generate "Send tagged PII data over HTTP and redact fields" \
     --provider openai \
     --model gpt-4-turbo \
     --few-shot-count 5
   ```
4. **查看输出**：若未指定 `--output`，生成的 CNL 代码将直接打印到终端，并附带验证结果、Token 统计与缓存命中信息。

> 完成一次生成后，可运行 `npm run ai:evaluate` 结合 `/tmp/phase3.4-systematic-test-results.json` 生成 `.claude/evaluation-report.md`，对比当前准确率与 Phase 3.4 指标。

## 命令选项
| 选项 | 说明 | 示例 |
| --- | --- | --- |
| `--provider <openai|anthropic>` | 指定底层 LLM Provider，默认为 OpenAI。 | `--provider anthropic` |
| `--model <name>` | 选择具体模型，影响提示长度与费用。 | `--model claude-3-5-sonnet-20241022` |
| `--temperature <0-1>` | 控制生成多样性。0.7 为默认，较低温度更稳定。 | `--temperature 0.4` |
| `--few-shot-count <n>` | 选择 Few-shot 示例数量，默认 5。 | `--few-shot-count 3` |
| `--output <file>` | 将结果写入文件而非控制台。 | `--output /tmp/dev-001.aster` |
| `--no-cache` | 禁用磁盘缓存，确保每次都调 LLM。 | `--no-cache` |

**提示**：缓存命中键格式为 `${provider}-${model}-temp${temperature}-fs${fewShotCount}-${hash(description)}`，文件位于 `.cache/ai-generation/`。

## 使用示例
```bash
# 生成代码并保存到文件
./dist/scripts/aster.js ai-generate "Join first and last name with a single space" \
  --provider openai \
  --model gpt-4-turbo \
  --output tmp/full-name.aster

# 在禁用缓存的情况下获取最新响应
./dist/scripts/aster.js ai-generate "Declare both HTTP and SQL capabilities" \
  --provider openai \
  --model gpt-4-turbo \
  --no-cache
```

## 提示词最佳实践
- **描述情境与输入输出**：写清楚输入来源（HTTP/SQL/PII 字段）与期望输出结构。
- **列出约束**：如需声明 capability、处理 PII、或遵循特定错误处理方式，请直白写出。
- **拆分步骤**：采用“分析 → 生成 → 验证”语句，帮助模型理解完整流程。
- **控制长度**：避免堆砌上下文；简洁且信息密度高的描述更容易命中缓存。
- **示例**：
  - ✅ `"Score a loan request by checking credit score >= 720, debt ratio <= 0.35, then assign risk: LOW/MED/HIGH"`
  - ❌ `"Handle loan scoring"`

## FAQ
- **问：命令提示缺少 API Key？**
  答：根据 provider 设置 `OPENAI_API_KEY` 或 `ANTHROPIC_API_KEY` 环境变量，重新运行命令即可。
- **问：如何验证准确率？**
  答：运行 `/tmp/run-systematic-tests.mjs` 生成结果，再执行 `npm run ai:evaluate`，即可在 `.claude/evaluation-report.md` 查看准确率（Phase 3.4 结果：80.0%，受 6 个 rate limit 错误影响）。
- **问：何时需要 `--no-cache`？**
  答：在 Few-shot 模板或描述频繁调整且需要最新响应时使用。默认启用缓存可节省 96% 再次运行时间。
- **问：是否支持多 provider？**
  答：目前提供 OpenAI、Anthropic 两种实现，新增 provider 需实现 `LLMProvider` 接口，详见架构文档。

## 故障排查
| 问题 | 现象 | 排查步骤 | 解决方法 |
| --- | --- | --- | --- |
| Rate limit (429) | CLI 输出 `Rate limit reached for gpt-4-turbo-preview` | 查看并发数（默认 3）、检查 token 使用量 | 等待限流重置、降低并发、切换模型、或拆分批次。|
| 缓存未命中 | 每次都调用 LLM，日志 `⚡ 緩存狀態: 未命中` | 确认描述完全一致、检查 `.cache/ai-generation/` 是否存在 | 复制描述文本，确保未启用 `--no-cache`，必要时重建缓存目录。|
| 验证未通过 | CLI 显示警告/错误列表 | 检查 `result.validation.diagnostics` 输出 | 按提示修改描述或手动编辑生成代码后再验证。|
| 输出乱码 | 终端语系导致 provenance 头出现字符异常 | 检查终端编码 | 使用 UTF-8 终端或将结果写入文件查看。|

## 相关资源
- Phase 3.4 实施总结：`.claude/phase3.4-completion-summary.md`
- 最新评估报告：`.claude/evaluation-report.md`
- 缓存实现：`src/ai/generation-cache.ts`
- 评估脚本：`scripts/evaluate-ai-generation.mjs`
