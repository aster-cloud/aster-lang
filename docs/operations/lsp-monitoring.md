> 更新时间：2025-11-27 14:55（NZDT）
> 执行者：Claude Code

# LSP 监控与运维指南

## 概述

Aster Language Server 提供健康检查和资源监控 API，用于生产环境部署和故障诊断。本文档描述如何使用监控 API、解读指标、设置告警以及排查常见故障。

## 访问方式

Health Check API 通过以下方式访问：

### 1. WebSocket JSON-RPC（当前支持）

Policy Editor 前端通过 WebSocket 连接到 `/ws/lsp` 端点，使用 JSON-RPC 2.0 协议调用 `aster/health` 方法。这是**当前唯一支持的访问方式**。

**前端状态指示器**：Policy Editor Monaco 编辑器组件内置 LSP 状态指示器，自动每 5 秒轮询健康状态并在 UI 显示。

**浏览器控制台调试**：
```javascript
// 在 Policy Editor 页面的浏览器控制台执行
// 获取 LSP 客户端实例并调用健康检查
const editor = document.querySelector('monaco-editor-component');
const health = await editor.lspClient.checkHealth();
console.log('LSP Health:', health);
```

### 2. REST API（计划中 - P2）

未来计划添加 REST 代理端点 `/api/lsp/health`，允许外部监控系统直接通过 HTTP 访问健康状态。当前此端点**尚未实现**。

### 3. CLI 直连测试

开发和调试时，可以通过 stdio 直接测试 LSP 健康检查：

```bash
# 启动 LSP 服务器并发送健康检查请求
echo '{"jsonrpc":"2.0","id":1,"method":"aster/health","params":{}}' | \
  node dist/src/lsp/server.js --stdio 2>/dev/null | \
  head -1 | jq '.result'
```

## Health Check API

### 端点信息

| 属性 | 值 |
|------|------|
| 协议 | JSON-RPC 2.0 (over WebSocket) |
| WebSocket 路径 | `/ws/lsp` |
| 方法 | `aster/health` |
| 参数 | `{}` (空对象) |

### 请求格式

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "aster/health",
  "params": {}
}
```

### 响应格式

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "watchers": {
      "capability": true,
      "registered": true,
      "mode": "dynamic",
      "isRunning": true,
      "trackedFiles": 150
    },
    "index": {
      "files": 869,
      "modules": 236
    },
    "queue": {
      "pending": 0,
      "running": 1,
      "completed": 42,
      "failed": 0,
      "total": 43
    },
    "process": {
      "pid": 12345,
      "uptime": 3600,
      "memory": {
        "rss": 150,
        "heapUsed": 80,
        "heapTotal": 120
      },
      "cpu": {
        "percent": 5.2
      }
    },
    "metadata": {
      "startTime": "2025-11-27T07:30:00.000Z",
      "restartCount": 3
    }
  }
}
```

## 指标说明

### 进程资源指标 (process)

| 指标路径 | 类型 | 单位 | 说明 | 正常范围 |
|----------|------|------|------|----------|
| `process.pid` | number | - | 进程 ID | > 0 |
| `process.uptime` | number | 秒 | 进程运行时间 | 递增，无上限 |
| `process.memory.rss` | number | MB | 常驻集大小（物理内存） | < 500 |
| `process.memory.heapUsed` | number | MB | 已使用堆内存 | < heapTotal × 0.8 |
| `process.memory.heapTotal` | number | MB | 堆内存总量 | 根据 Node.js 配置 |
| `process.cpu.percent` | number | % | CPU 使用率（采样间隔计算） | < 50 |

### 元数据指标 (metadata)

| 指标路径 | 类型 | 格式 | 说明 |
|----------|------|------|------|
| `metadata.startTime` | string | ISO 8601 | 服务器启动时间 |
| `metadata.restartCount` | number | 整数 | 历史累计重启次数 |

### 索引指标 (index)

| 指标路径 | 类型 | 说明 |
|----------|------|------|
| `index.files` | number | 已索引的文件数量 |
| `index.modules` | number | 已解析的模块数量 |

### 队列指标 (queue)

| 指标路径 | 类型 | 说明 |
|----------|------|------|
| `queue.pending` | number | 等待处理的任务数 |
| `queue.running` | number | 正在执行的任务数 |
| `queue.completed` | number | 已完成的任务数 |
| `queue.failed` | number | 失败的任务数 |
| `queue.total` | number | 任务总数 |

### 文件监视器指标 (watchers)

| 指标路径 | 类型 | 说明 |
|----------|------|------|
| `watchers.capability` | boolean | 客户端是否支持文件监视 |
| `watchers.registered` | boolean | 监视器是否已注册 |
| `watchers.mode` | string | 监视模式 (dynamic/static) |
| `watchers.isRunning` | boolean | 监视器是否正在运行 |
| `watchers.trackedFiles` | number | 正在监视的文件数量 |

## 告警阈值建议

### 警告级别 (WARNING)

| 条件 | 说明 | 建议操作 |
|------|------|----------|
| `memory.rss > 500 MB` | 内存使用偏高 | 观察趋势，准备重启 |
| `cpu.percent > 50%` | CPU 使用偏高 | 检查是否有大量文件变更 |
| `restartCount` 增长 > 5 次/小时 | 频繁重启 | 检查错误日志 |
| `queue.pending > 100` | 任务积压 | 检查 LSP 是否过载 |
| `queue.failed > 10` | 任务失败较多 | 检查失败原因 |

### 严重级别 (CRITICAL)

| 条件 | 说明 | 建议操作 |
|------|------|----------|
| `memory.rss > 1 GB` | 内存严重超标 | 立即重启，排查泄漏 |
| `cpu.percent > 80%` 持续 5 分钟 | CPU 持续高负载 | 检查死循环或性能瓶颈 |
| Health check 无响应超过 30 秒 | 服务可能卡死 | 强制重启，收集诊断信息 |
| `uptime < 60` 且 `restartCount` 递增 | 启动失败循环 | 检查配置和依赖 |

## 常见故障排查

### 1. LSP 连接失败

**症状**: Policy Editor 无法连接到 LSP，状态显示 "error" 或 "disconnected"

**排查步骤**:

1. 检查 LSP 服务器文件是否存在：
   ```bash
   ls -lh dist/src/lsp/server.js
   ```

2. 检查 Node.js 版本（需要 >= 22）：
   ```bash
   node --version
   ```

3. 手动测试 LSP 启动：
   ```bash
   node dist/src/lsp/server.js --stdio
   # 应该等待输入，无立即报错
   ```

4. 检查 Policy Editor 日志：
   ```bash
   # Quarkus 日志
   tail -f /var/log/policy-editor/application.log
   # 或使用 journalctl
   journalctl -u policy-editor -f
   ```

5. 检查 WebSocket 端点状态：
   ```bash
   curl -v http://localhost:8080/ws/lsp
   # 应该返回 400 Bad Request（WebSocket 升级失败）
   ```

### 2. 内存持续增长

**症状**: `process.memory.rss` 持续增长，超过 500 MB 或更高

**排查步骤**:

1. 通过浏览器控制台记录内存趋势：
   ```javascript
   // 在 Policy Editor 页面执行
   const editor = document.querySelector('monaco-editor-component');
   setInterval(async () => {
     const health = await editor.lspClient.checkHealth();
     console.log('Memory:', health.process?.memory);
   }, 60000);
   ```

2. 检查索引文件数量（通过 UI 状态指示器或控制台）：
   ```javascript
   const editor = document.querySelector('monaco-editor-component');
   const health = await editor.lspClient.checkHealth();
   console.log('Index files:', health.index?.files);
   // 如果 > 1000，考虑缩小工作区范围
   ```

3. 检查队列积压：
   ```javascript
   const editor = document.querySelector('monaco-editor-component');
   const health = await editor.lspClient.checkHealth();
   console.log('Queue:', health.queue);
   // pending 持续增长可能导致内存增长
   ```

4. 临时解决方案 - 重启 LSP：
   - 关闭并重新打开 Policy Editor
   - 或通过 WebSocket 重新连接

5. 长期解决方案：
   - 配置定期重启（建议 uptime > 24h 时重启）
   - 缩小工作区文件范围
   - 增加服务器内存

### 3. 连接数达到上限

**症状**: 新用户无法打开 Policy Editor，WebSocket 连接被拒绝（CloseReason 1013: TRY_AGAIN_LATER）

**排查步骤**:

1. 检查当前活跃连接数：
   ```java
   // 在 Policy Editor 后端日志中查看，或通过 JMX/调试器访问
   // LSPWebSocketEndpoint.getActiveConnectionCount()
   // 日志示例: "LSP WebSocket 已连接: xxx, 活跃连接数: 3/10"
   ```

   也可在 Quarkus Dev UI（开发模式）或应用日志中查看连接状态。

2. 查看配置的最大连接数：
   ```bash
   grep "lsp.max.concurrent.connections" application.properties
   ```

3. 排查僵尸连接：
   - 检查是否有浏览器标签页未正确关闭
   - 检查网络中断后连接未释放

4. 调整连接限制（如有必要）：
   ```properties
   # application.properties
   lsp.max.concurrent.connections=20
   ```

5. 强制清理（紧急情况）：
   - 重启 Policy Editor 服务
   - 所有连接将被优雅关闭

### 4. LSP 响应缓慢

**症状**: 编辑器操作（补全、跳转、格式化）响应延迟超过 2 秒

**排查步骤**:

1. 检查 CPU 使用率（通过浏览器控制台）：
   ```javascript
   const editor = document.querySelector('monaco-editor-component');
   const health = await editor.lspClient.checkHealth();
   console.log('CPU:', health.process?.cpu?.percent, '%');
   // 如果 > 80%，可能有性能瓶颈
   ```

2. 检查队列状态：
   ```javascript
   const editor = document.querySelector('monaco-editor-component');
   const health = await editor.lspClient.checkHealth();
   console.log('Queue:', health.queue);
   // running > 1 表示有并发处理
   // pending > 10 表示请求积压
   ```

3. 检查索引大小：
   ```javascript
   const editor = document.querySelector('monaco-editor-component');
   const health = await editor.lspClient.checkHealth();
   console.log('Index:', health.index);
   // files > 1000 可能影响性能
   ```

4. 临时解决方案：
   - 关闭不必要的编辑器标签页
   - 减少同时编辑的文件数量

5. 长期优化：
   - 限制工作区范围
   - 增加服务器 CPU/内存资源
   - 优化 LSP 索引配置

### 5. 重启计数器异常增长

**症状**: `metadata.restartCount` 快速增长，表明 LSP 进程频繁崩溃

**排查步骤**:

1. 查看重启计数趋势：
   ```bash
   # 查看重启计数器文件
   cat /tmp/lsp-restart-count.txt
   ```

2. 检查 LSP 进程退出原因：
   ```bash
   # 查看 Policy Editor 日志中的 LSP stderr
   grep "LSP stderr" /var/log/policy-editor/application.log | tail -20
   ```

3. 常见崩溃原因：
   - 内存不足 (OOM)
   - 未处理的异常
   - Node.js 版本不兼容

4. 解决方案：
   - 增加 Node.js 内存限制：`NODE_OPTIONS="--max-old-space-size=4096"`
   - 更新到最新的 LSP 服务器版本
   - 检查并修复触发崩溃的特定文件

## 配置参数

### Backend 配置 (application.properties)

```properties
# LSP 最大并发连接数
# 默认值: 10
# 建议: 4核8GB=10, 8核16GB=20
lsp.max.concurrent.connections=10

# LSP 关闭超时时间（秒）
# 默认值: 30
# 说明: shutdown 请求后等待进程退出的最长时间
lsp.shutdown.timeout.seconds=30
```

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `LSP_RESTART_COUNTER_FILE` | `/tmp/lsp-restart-count.txt` | 重启计数器持久化文件路径 |
| `NODE_OPTIONS` | - | Node.js 运行时选项，如 `--max-old-space-size=4096` |

### Frontend LSP 配置

Frontend 状态指示器默认配置：
- 健康检查间隔: 5 秒（带节流）
- 重连延迟: 指数退避，最大 30 秒
- 状态显示: 连接/正在连接/断开/错误

## 监控集成

### 当前监控方式

由于 Health Check API 当前仅支持 WebSocket JSON-RPC 访问，外部监控系统有以下选择：

1. **应用日志监控**：解析 Policy Editor 日志中的连接状态信息
2. **前端 UI 观察**：通过 LSP 状态指示器直接查看
3. **Quarkus Dev UI**：开发模式下查看应用状态

### CLI 健康检查脚本示例

```bash
#!/bin/bash
# lsp-health-check.sh
# 通过 stdio 直接测试 LSP 服务器健康状态

ALERT_THRESHOLD_MEMORY=500
ALERT_THRESHOLD_CPU=80

# 发送健康检查请求并获取响应
response=$(echo '{"jsonrpc":"2.0","id":1,"method":"aster/health","params":{}}' | \
  timeout 10 node dist/src/lsp/server.js --stdio 2>/dev/null | \
  head -1)

if [ -z "$response" ]; then
  echo "CRITICAL: LSP health check timeout or failed to start"
  exit 2
fi

# 解析响应
memory=$(echo "$response" | jq -r '.result.process.memory.rss // 0')
cpu=$(echo "$response" | jq -r '.result.process.cpu.percent // 0')

if (( $(echo "$memory > $ALERT_THRESHOLD_MEMORY" | bc -l) )); then
  echo "WARNING: Memory usage high: ${memory}MB"
  exit 1
fi

if (( $(echo "$cpu > $ALERT_THRESHOLD_CPU" | bc -l) )); then
  echo "WARNING: CPU usage high: ${cpu}%"
  exit 1
fi

echo "OK: Memory=${memory}MB, CPU=${cpu}%"
exit 0
```

> **注意**: 此脚本启动独立的 LSP 进程进行测试，不反映 Policy Editor 中运行的 LSP 实例状态。
> 生产环境监控应等待 REST API 代理实现（P2 计划）。

### Prometheus 导出（未来计划 - P2）

计划添加 `/metrics` 端点，导出 Prometheus 格式指标：

```
# HELP lsp_process_uptime_seconds LSP server uptime in seconds
# TYPE lsp_process_uptime_seconds gauge
lsp_process_uptime_seconds 3600

# HELP lsp_process_memory_bytes LSP server memory usage
# TYPE lsp_process_memory_bytes gauge
lsp_process_memory_bytes{type="rss"} 157286400
lsp_process_memory_bytes{type="heapUsed"} 83886080
lsp_process_memory_bytes{type="heapTotal"} 125829120

# HELP lsp_process_cpu_percent LSP server CPU usage percentage
# TYPE lsp_process_cpu_percent gauge
lsp_process_cpu_percent 5.2

# HELP lsp_connections_active Current number of active WebSocket connections
# TYPE lsp_connections_active gauge
lsp_connections_active 3

# HELP lsp_restarts_total Total number of LSP server restarts
# TYPE lsp_restarts_total counter
lsp_restarts_total 5
```

## 性能优化建议

### 内存优化

1. **定期重启长时间运行的 LSP 进程**
   - 建议: uptime > 24 小时时安排重启
   - 方式: 通过 shutdown/exit LSP 协议或重启 Policy Editor 服务

2. **限制索引文件数量**
   - 建议: < 1000 个 .aster 文件
   - 配置: 使用 `.asterignore` 排除不需要索引的目录

3. **调整 Node.js 内存限制**
   ```bash
   NODE_OPTIONS="--max-old-space-size=2048" # 2GB
   ```

### 并发优化

1. **根据服务器资源调整连接限制**

   | 服务器配置 | 建议连接数 |
   |-----------|-----------|
   | 2 核 4GB | 5 |
   | 4 核 8GB | 10 |
   | 8 核 16GB | 20 |
   | 16 核 32GB | 40 |

2. **避免连接泄漏**
   - 确保浏览器关闭时 WebSocket 正确断开
   - 网络异常时前端自动重连，后端超时清理

### 监控频率

| 场景 | 建议间隔 |
|------|---------|
| 前端状态指示器 | 5 秒 |
| 外部健康检查 | 30 秒 |
| 指标采集 (Prometheus) | 15 秒 |
| 告警评估 | 1 分钟 |

## 相关文档

- [LSP 格式化与 CLI 指南](../guide/formatting.md) - LSP 基本使用
- [故障排查手册](./troubleshooting.md) - 通用故障排查
- [配置指南](./configuration.md) - 完整配置参考
- [部署指南](./deployment.md) - 生产环境部署
