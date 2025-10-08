> 更新时间：2025-10-08 14:57（NZDT）  
> 执行者：Codex

# 故障排查手册

## 1. 常见错误与解决方案
| 错误 ID | 触发场景 | 解决步骤 |
| --- | --- | --- |
| `E1001`（CAPABILITY_VIOLATION） | 函数声明的能力与 manifest 不匹配 | 更新 manifest（`ASTER_CAPS`）允许对应模块/函数，或调整函数效果声明 |
| `E1003`（CAPABILITY_MISSING） | 模块缺少必要能力声明 | 补充 `It performs IO/CPU`，或放宽 manifest |
| `E2001`（TYPE_MISMATCH） | 类型推断失败 | 检查 CNL 源代码类型一致性，必要时启用 `ASTER_DEBUG_TYPES=1` 获取解析日志 |
| `E3002`（EFFECT_SUPERFLUOUS） | 声明了未使用的效应 | 移除冗余效果或加入实际 IO/CPU 调用 |
| `E4001`（PARSER_UNEXPECTED_TOKEN） | 语法错误或缩进不对齐 | 使用 `npm run fmt:examples` 自动修正格式 |
| `E5002`（LEXER_INVALID_INDENTATION） | 缩进混用 Tab | 将文件转换为空格缩进，重新运行 CLI |
| `E9001`（INTERNAL_UNEXPECTED_STATE） | 理论上不应出现的内部错误 | 收集日志（含堆栈），开 issue 并附带触发输入 |

> 日志中会输出 `code`, `timestamp`, `component` 字段，便于跨系统检索。

## 2. 日志查看方法
1. 所有核心模块使用结构化 JSON 日志，默认最小级别 `INFO`。
2. 本地调试：
   ```bash
   LOG_LEVEL=DEBUG node dist/scripts/cli.js cnl/examples/greet.cnl | jq
   ```
3. 服务器排障：
   - 日志行均为单行 JSON，采集时按行分割即可。
   - 关注字段：`level`、`component`、`message`、附加 `meta`（如 `duration_ms`）。
4. 性能日志：
   ```bash
   node -e "const { logPerformance } = require('./dist/utils/logger.js'); logPerformance({ component: 'typecheck', operation: 'module', duration: 128 });"
   ```
   - 输出示例：`{"level":"INFO","timestamp":"...","component":"performance","message":"module completed","duration_ms":128}`。

## 3. 性能问题排查
- **构建缓慢**：确认 `npm ci` 使用缓存（CI 已启用）；本地可开启 `npm run build -- --incremental`（TypeScript 会复用缓存）。
- **Typecheck 卡顿**：检查 manifest 是否包含过多通配符，可拆分细粒度 allow/deny；必要时提升机器内存。
- **Benchmark 指标下降**：
  ```bash
  npm run bench
  ```
  - 对比 benchmark 输出与历史记录，若异常可通过 `git bisect` 定位。
- **Truffle 执行慢**：关闭 `ASTER_TRUFFLE_DEBUG`，并检查 `JAVA_OPTS` 是否设置足够堆大小。

## 4. 安全相关问题
- 依赖安全扫描：
  ```bash
  npm run audit          # audit-ci --moderate
  npx audit-ci --high    # CI security workflow 同步命令
  ```
- NPM 凭证：
  - 确认 `NODE_AUTH_TOKEN` 写入环境且未泄漏至日志。
  - 发布失败时检查 npm token 权限是否为 `automation`。
- 能力校验：
  - 确保 `ASTER_CAP_EFFECTS_ENFORCE` 未被关闭。
  - 在日志管道中监控 `E1001/E1002` 频率，异常升高时提醒研发。

## 5. LSP 排障
- **LSP 无法启动**：
  ```bash
  npm run build
  node dist/src/lsp/server.js --stdio
  ```
  - 观察日志是否有 `E` 级别条目。
- **能力提示缺失**：确认启动前导出 `ASTER_CAPS=/path/to/manifest.json`。
- **索引异常**：
  ```bash
  npm run test:lsp-index
  ```
  - 若失败，清理缓存目录 `~/.cache/aster-lsp`（如存在）。
- **VS Code 调试**：使用扩展仓库 `editors/vscode/aster-vscode`，启用 `CI_DEBUG=1` 运行 `npm run ci` 可保留详细输出。

## 6. 调试技巧
- 启用解析调试：
  ```bash
  ASTER_DEBUG_TYPES=1 node dist/scripts/cli.js cnl/examples/map.cnl
  ```
- 追踪特定函数的能力检查：
  ```bash
  LOG_LEVEL=DEBUG node dist/scripts/typecheck-cli.js cnl/examples/capdemo.cnl
  ```
- Node Inspector：
  ```bash
  node --inspect-brk dist/scripts/cli.js cnl/examples/greet.cnl
  ```
- 快速验证 manifest 变更：
  ```bash
  ASTER_CAPS=/tmp/capabilities.json npm run test:golden
  ```
- 收集详细栈信息：使用 `LOG_LEVEL=DEBUG` 并确认 `error` 日志包含 `stack` 字段。
