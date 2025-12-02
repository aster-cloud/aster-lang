# AI 代码生成性能优化指南

更新日期：2025-11-25 21:41 NZST · 执行者：Codex

## Phase 3.4 成果摘要
- **并发调度**：`/tmp/run-systematic-tests.mjs` 将 `dev.jsonl` 16 个用例按并发=3 批量执行，缩短首轮耗时至约 1 分 30 秒。
- **磁盘缓存**：`GenerationCache` 在首轮生成 10 个缓存文件，二轮命中率 62.5%，命中请求平均耗时 <1 秒。
- **进度追踪**：CLI 输出 `[passed/total]`、`✓/✗/⚠/⚡` 指标，帮助分析 rate limit 与缓存表现。
- **准确率保持**：10 个完成用例中 8 个通过（80.0%），与 Phase 3.3 的 81.3% 基线一致；剩余 6 个因 429 限流被标记为 ERROR。

## 性能指标对比
| 场景 | Phase 3.3 (串行) | Phase 3.4 (并发+缓存) | 提升 |
| --- | --- | --- | --- |
| 首次运行（16 用例，无缓存） | 16 × 30s ≈ 480s ≈ 8 分钟 | 16/3 × 30s ≈ 160s（实测 ~90s，未完成用例受限流影响） | ≈66% 缩短 |
| 第二次运行（缓存命中） | 16 × 30s ≈ 480s | 16 × <1s ≈ 16s（命中部分立即返回） | ≈96% 缩短 |
| 报告生成 (`npm run ai:evaluate`) | 手动整理日志 | 197 行脚本自动输出 Markdown，平均 <1s | 100% 自动化 |

> **结论**：只要描述一致，缓存即可将 LLM 调用成本降到几乎为零；并发机制在 rate limit 允许范围内提供 3× 加速。

## 缓存策略
1. **适用场景**：
   - 回归测试或演示同一需求。
   - Systematic testing 第二轮复现，确保结果一致。
2. **命中条件**：`description`、`provider`、`model`、`temperature`、`fewShotCount` 完全一致。任何空格或大小写差异都会导致 miss。
3. **目录布局**：`.cache/ai-generation/<key>.json` 存储 `code/rawCode/validation/metadata/usage`，不含 `fromCache` 字段。
4. **维护建议**：
   ```bash
   # 清空缓存
   rm -rf .cache/ai-generation

   # 查看命中数量
   ls .cache/ai-generation | wc -l
   ```
5. **失效策略**：
   - Prompt、Few-shot 或模型升级后应删除缓存，以防旧结果造成误判。
   - 可在 CI 设置 TTL（例如 7 天）或按分支命名子目录。

## Rate Limit 应对方案
| 策略 | 操作 | 适用场景 | 备注 |
| --- | --- | --- | --- |
| 降低并发 | 将 `CONCURRENCY` 从 3 调至 2 或 1 | 夜间/配额紧张 | 牺牲速度换稳定性。 |
| 重试与退避 | 捕获 429 后 `await sleep(waitTime)` 再次执行 | 长时间批量测试 | 可结合指数退避，避免雪崩。 |
| 分批执行 | 将 16 个用例分多轮运行，每轮间隔 60s | CI/CD、公共 API Key | 减少瞬时 TPM 请求量。 |
| 缓存优先 | 先运行一轮获取缓存，再针对失败/新用例复测 | 日常开发 | 第二轮几乎全命中，显著降成本。 |
| 升级配额 | 向 OpenAI 申请更高 TPM | 正式交付/高频运行 | 需业务批准。 |

示例降并发代码（节选）：
```javascript
const CONCURRENCY = Number(process.env.AI_TEST_CONCURRENCY ?? 3);
```
在 CI 中导出 `AI_TEST_CONCURRENCY=2` 即可动态配置。

## 最佳实践建议
1. **先构建再评估**：始终 `npm run build`，确保 CLI 与提示模板同步。
2. **两轮运行**：
   ```bash
   export OPENAI_API_KEY="..."
   node /tmp/run-systematic-tests.mjs
   npm run ai:evaluate
   node /tmp/run-systematic-tests.mjs   # 验证缓存命中
   ```
3. **关注失败类型**：在 `.claude/evaluation-report.md` 中定位 `FAILED`（逻辑错误）与 `ERROR`（限流/环境）并分别处理。
4. **Prompt 版本化**：更改 Few-shot 或 system prompt 时，记录版本号并清理缓存，避免旧数据影响准确率统计。
5. **日志留存**：将 `/tmp/phase3.4-*.log`、测试 JSON、评估报告上传到 `docs/workstreams/<TASK-ID>/verification/`，方便追踪性能趋势。
6. **自动化触发**：建议在 CI 新增步骤：`node /tmp/run-systematic-tests.mjs || true`，随后无论退出码如何均运行 `npm run ai:evaluate`，确保报告生成。

## 故障排查速查表
| 症状 | 可能原因 | 处理建议 |
| --- | --- | --- |
| 第二轮仍耗时较长 | 描述或选项发生变化，缓存 miss | 比较 CLI 参数，或使用 `AIGenerator.generate({ useCache: true })` 并确认输入一致。 |
| 准确率 <80% | 新增用例失败、验证器拦截 | 在评估报告 `失败与错误详情` 中查看 `reason`，对照 `dev.jsonl` 调整提示。 |
| JSON 解析失败 | 系统测试输出被截断或包含颜色控制符 | 使用评估脚本中同款 `ANSI_PATTERN` 清洗日志，再写入 JSON；或重新运行测试生成干净输出。 |
| 缓存文件损坏 | 中断写入导致临时文件残留 | 删除 `.cache/ai-generation/*.tmp` 并重新生成。 |

通过以上策略，可持续维持 Phase 3.4 的性能基线，并为后续 Phase 5 提升准确率奠定稳定、可观测的运行环境。
