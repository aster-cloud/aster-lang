# 结构化日志系统使用指引

日期：2025-10-08 14:50 NZST  
执行者：Codex

## Logger API
- `createLogger(component: string)`：根据 `LOG_LEVEL` 环境变量创建命名组件的 `Logger` 实例；支持的级别为 `DEBUG/INFO/WARN/ERROR`，默认为 `INFO`。（参考 `src/utils/logger.ts:1`）
- `logger.debug/info/warn(message, meta?)`：输出 JSON 日志，`meta` 字段会被展开到顶层，便于下游检索。（参考 `src/utils/logger.ts:10`）
- `logger.error(message, error?, meta?)`：在保留堆栈与错误信息的同时附加自定义元数据。（参考 `src/utils/logger.ts:22`）
- `logPerformance({ component, operation, duration, metadata })`：快速记录性能指标，内部使用 `Logger('performance')`，自动包含 `duration_ms`。（参考 `src/utils/logger.ts:47`）

## 环境变量与级别
- `LOG_LEVEL`：接受 `DEBUG/INFO/WARN/ERROR`（大小写不敏感），决定 `Logger` 输出的最低级别。（参考 `src/utils/logger.ts:57`）
- 建议在开发环境设为 `DEBUG` 以启用 `parser` 等模块的调试日志；生产默认保持 `INFO` 避免噪声。

## 集成示例
- 类型检查入口在 `src/typecheck.ts:139` 使用 `createLogger('typecheck')` 打点模块开始/结束，并通过 `logPerformance` 记录耗时与诊断数量。
- 类型等值检查 `tEquals` 将原有 `console.warn` 替换为 `typecheckLogger.warn`，在元数据中附上未处理类型（`src/typecheck.ts:62`）。
- 解析器调试日志走向 `parserLogger.debug`，并保留嵌套深度信息（`src/parser.ts:59`），与环境变量 `ASTER_DEBUG_TYPES=1` 配合使用。

## 扩展指引
- 处理 `DiagnosticError` 或 `createError` 生成的结构化错误时，可将 `code/timestamp/location` 放入 `meta`，保持诊断与日志统一编号。
- 执行性能敏感操作（如 LSP 索引构建）时，可在操作前后记录 `performance.now()` 差值并调用 `logPerformance`。
- 若需要保留面向 LSP 客户端的 `connection.console` 输出，可在调用后追加 `logger.info` 以同步写入集中日志。

## 验证流程
1. `npm run typecheck` 确认 TypeScript 编译通过。
2. `npm run test` 观察黄金测试期间输出的 JSON 日志，确认性能指标与诊断数量字段存在。
3. `LOG_LEVEL=DEBUG node dist/scripts/typecheck-cli.js <文件>` 校验命令行工具输出结构化日志；如需 parser 调试信息，可额外设置 `ASTER_DEBUG_TYPES=1`。
