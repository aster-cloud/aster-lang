# 性能基准系统

> 更新时间：2025-10-09
> 文档范围：编译性能 + LSP 性能基准测试

## 概述

本项目使用三层测试规模（Small / Medium / Large）对编译管道（canonicalize → lex → parse → lower）和 LSP 端到端性能（hover / completion / diagnostics）进行持续监控，通过动态阈值与回归检测机制防止性能退化。

## 快速开始

### 运行基准测试

```bash
# 完整基准测试（编译 + LSP）
npm run perf:benchmark

# 单独验证报告
npm run perf:report

# CI 流程（自动执行）
npm run ci  # 包含 perf:benchmark（非阻塞模式）
```

### 查看结果

基准测试完成后，会生成 `perf-report.json` 文件，包含：
- 编译各阶段性能指标（p50/p95/p99）
- LSP 操作延迟分布
- 阈值对比与回归检测结果
- 通过/失败状态

## 测试规模定义

| 规模 | 文件数 | 代码行数 | 典型特征 | 用途 |
|------|--------|---------|---------|------|
| **Small** | 1 | ~10 | 单文件简单函数（greet.aster） | 快速反馈，基础性能 |
| **Medium** | 40 | ~3200 | 多模块业务逻辑（生成项目） | 模块间依赖，类型推导 |
| **Large** | 1 | ~350 | 高复杂度单模块（50函数） | 解析器/类型系统极限 |

### Small 项目
- 示例：`cnl/examples/greet.aster`
- 用途：检测编译管道基础性能
- 特点：无依赖、单个函数

### Medium 项目
- 生成方式：`generateMediumProject(40)` (test/generators.ts)
- 结构：40 个模块，包含：
  - Record 类型（User/Config/Request/Profile）
  - Sum 类型（Status/ErrorKind/ResultFlag）
  - 每模块 8-12 个函数
  - 10% 函数带 @io effect
  - 30% 模块有 Use 依赖
- 用途：模拟中型业务系统

### Large 项目
- 生成方式：`generateLargeProgram(50)` (test/generators.ts)
- 结构：单模块 50 个函数，包含：
  - 深层嵌套控制流（For Each / Match）
  - 复杂类型推导
  - 高函数调用密度
- 用途：压力测试解析器和类型检查器

## 性能指标说明

### 编译性能

| 阶段 | 说明 | Small 基线 (p50) | Medium 基线 (p50) |
|------|------|-----------------|-------------------|
| **canonicalize** | Unicode 规范化 | ~0.03 ms | ~7 ms |
| **lex** | 词法分析 | ~0.03 ms | ~7 ms |
| **parse** | 语法解析 | ~0.12 ms | ~6.5 ms |
| **lower** | AST → Core IR | ~0.06 ms | ~1.2 ms |
| **pipeline** | 完整编译流程 | ~0.28 ms | ~21 ms |

### LSP 性能

| 操作 | 说明 | Small 基线 (p95) | 阈值 |
|------|------|-----------------|------|
| **hover** | 鼠标悬停提示 | 5000 ms (超时) | 100 ms |
| **completion** | 自动补全 | ~2 ms | 150 ms |
| **diagnostics** | 诊断推送 | ~0 ms (缓存) | N/A |

**已知问题**: LSP hover 请求当前存在性能问题（始终超时），已在 CI 中设为非阻塞模式，不影响编译性能验证。

## 环境变量配置

### 编译基准配置

```bash
# 迭代次数（影响统计精度）
COMPILATION_SMALL_ITERATIONS=100   # 默认 100（Small 项目快速执行）
COMPILATION_MEDIUM_ITERATIONS=12   # 默认 12（Medium 项目中等复杂度）
COMPILATION_LARGE_ITERATIONS=40    # 默认 40（Large 项目高复杂度）

# 阈值配置
PERF_THRESHOLD_PARSE_P50_MS=30           # parse p50 阈值（毫秒）
PERF_THRESHOLD_PIPELINE_P50_MS=5         # pipeline p50 阈值（毫秒）
PERF_THRESHOLD_LSP_HOVER_P95_MS=100      # LSP hover p95 阈值（毫秒）
PERF_THRESHOLD_LSP_COMPLETION_P95_MS=150 # LSP completion p95 阈值（毫秒）

# 回归容忍度
PERF_BASELINE_MARGIN=0.15      # 基线阈值浮动范围（默认 15%）
PERF_REGRESSION_TOLERANCE=0.2  # 回归检测容忍度（默认 20%）
```

### LSP 基准配置

```bash
# 请求迭代次数
LSP_PERF_ITERATIONS=100              # hover/completion 迭代次数（默认 100）
LSP_PERF_DIAG_ITERATIONS=20          # diagnostics 迭代次数（默认 20）

# 超时配置
LSP_PERF_TIMEOUT_MS=5000             # 请求超时（默认 5000ms）
LSP_PERF_DIAG_TIMEOUT_MS=5000        # 诊断超时（默认 5000ms）

# 调试模式
LSP_PERF_DEBUG=1                     # 启用调试日志
```

### CI 专用配置

```bash
# 快速验证模式（减少迭代次数）
LSP_PERF_ITERATIONS=10 \
LSP_PERF_DIAG_ITERATIONS=5 \
COMPILATION_SMALL_ITERATIONS=50 \
COMPILATION_MEDIUM_ITERATIONS=5 \
COMPILATION_LARGE_ITERATIONS=10 \
npm run perf:benchmark
```

## 报告格式 (perf-report.json)

### 顶层结构

```json
{
  "timestamp": "2025-10-09T00:00:00.000Z",
  "metadata": {
    "node_version": "v22.14.0",
    "platform": "darwin",
    "arch": "arm64",
    "cpus": 10,
    "total_memory_gb": 64
  },
  "compilation": { ... },
  "lsp": { ... },
  "thresholds": { ... },
  "passed": true,
  "failures": []
}
```

### compilation 字段

每个规模包含以下指标：

```json
{
  "small": {
    "files": 1,
    "lines": 9,
    "canonicalize_ms": { "p50": 0.026, "p95": 0.648, "p99": 0.648 },
    "lex_ms": { "p50": 0.034, "p95": 0.455, "p99": 0.455 },
    "parse_ms": { "p50": 0.123, "p95": 1.596, "p99": 1.596 },
    "lower_ms": { "p50": 0.058, "p95": 0.446, "p99": 0.446 },
    "pipeline_ms": { "p50": 0.276, "p95": 3.148, "p99": 3.148 }
  }
}
```

### lsp 字段

每个项目规模包含：

```json
{
  "small": {
    "files": 1,
    "lines": 9,
    "initialize_ms": 67.65,
    "hover": { "p50": 1.2, "p95": 3.5, "p99": 5.8 },
    "completion": { "p50": 1.4, "p95": 2.1, "p99": 2.5 },
    "diagnostics": { "p50": 0, "p95": 0, "p99": 0 }
  }
}
```

### thresholds 字段

动态阈值由以下公式计算：

```
threshold = max(default_threshold, baseline_value × (1 + BASELINE_MARGIN))
```

示例：

```json
{
  "parse_p50_ms": 30,
  "greet_pipeline_ms": 5,
  "lsp_hover_p95_ms": 5750,
  "lsp_completion_p95_ms": 150
}
```

### failures 字段

失败原因分为两类：

**阈值失败**：当前值超过绝对阈值
```json
[
  "Small parse p50 0.12ms 超过阈值 0.10ms"
]
```

**回归失败**：当前值超过基线值 × (1 + tolerance)
```json
[
  "Small parse p50 从 0.02ms 上升到 0.12ms（允许上限 0.02ms）"
]
```

## 故障排查

### 1. LSP hover 始终超时

**现象**：报告中所有 hover 延迟显示为 5000ms

**原因**：LSP 服务器 hover 功能存在性能问题

**解决方案**：
- **短期**：CI 已配置为非阻塞模式，不影响流水线
- **中期**：调整 `LSP_PERF_TIMEOUT_MS` 延长超时
- **长期**：优化 LSP 服务器 hover 实现（Stage 3.1 LSP 架构重构）

```bash
# 临时延长超时
LSP_PERF_TIMEOUT_MS=10000 npm run perf:benchmark
```

### 2. 大量回归检测失败

**现象**：报告中出现多条 "从 X ms 上升到 Y ms" 失败

**原因**：
- 硬件差异（CI vs 本地）
- 系统负载波动
- 测量误差（迭代次数不足）

**解决方案**：

```bash
# 1. 增加迭代次数以减少误差
COMPILATION_SMALL_ITERATIONS=200 npm run perf:benchmark

# 2. 调整容忍度（仅用于诊断）
PERF_REGRESSION_TOLERANCE=0.3 npm run perf:benchmark

# 3. 重置基线（确认性能合理后）
npm run perf:benchmark  # 生成新基线
git add perf-report.json
git commit -m "chore: update performance baseline"
```

### 3. Medium 项目生成不稳定

**现象**：Medium 项目性能指标波动大

**原因**：`generateMediumProject()` 使用伪随机生成，种子固定但代码结构随机

**解决方案**：
- 默认种子（42）已确保可重现性
- 如需更改项目规模，修改 `scripts/perf-benchmark.ts`:

```typescript
const mediumProject = generateMediumProject(40, 42); // 40 模块，种子 42
```

### 4. JSON 解析失败

**现象**：`perf:report` 报错 "无法解析 perf-report.json"

**原因**：
- perf-report.json 损坏
- 上次基准测试未完成

**解决方案**：

```bash
# 删除损坏的报告并重新运行
rm perf-report.json
npm run perf:benchmark
```

### 5. 编译阶段异常慢

**现象**：Small 项目 parse p50 超过 10ms（正常 ~0.1ms）

**诊断步骤**：

```bash
# 1. 检查系统资源
node -e "console.log(process.memoryUsage())"

# 2. 单独运行编译基准（无 LSP）
node dist/test/benchmark.js

# 3. 启用调试模式
LSP_PERF_DEBUG=1 npm run perf:benchmark
```

## 最佳实践

### 开发流程

1. **功能开发前**：运行基准测试建立基线
   ```bash
   npm run perf:benchmark
   git add perf-report.json
   git commit -m "chore: baseline before feature X"
   ```

2. **功能开发中**：定期运行快速验证
   ```bash
   COMPILATION_SMALL_ITERATIONS=20 npm run perf:benchmark
   ```

3. **提交前**：完整基准测试
   ```bash
   npm run perf:benchmark
   npm run perf:report  # 确认通过
   ```

### CI 集成

项目已在 `package.json` 中配置：

```json
{
  "scripts": {
    "ci": "... && ((npm run perf:benchmark && npm run perf:report) || echo 'perf-benchmark non-blocking') && ..."
  }
}
```

**非阻塞模式**：即使基准测试失败，CI 流程仍继续（用于 LSP hover 超时问题）

**严格模式**（推荐用于生产环境）：

```bash
npm run perf:benchmark && npm run perf:report  # 失败时中断流程
```

### 性能优化指南

1. **识别瓶颈**：查看报告中 p95/p99 偏离 p50 最大的阶段
2. **聚焦高优先级**：优先优化 Small 项目性能（影响开发体验）
3. **验证改进**：对比前后两次基线报告

```bash
# 优化前
npm run perf:benchmark
mv perf-report.json perf-report.before.json

# 实施优化
# ...

# 优化后
npm run perf:benchmark

# 对比结果
node -e "
const before = require('./perf-report.before.json');
const after = require('./perf-report.json');
console.log('Small parse p50:', before.compilation.small.parse_ms.p50, '→', after.compilation.small.parse_ms.p50);
"
```

## 相关文件

### 核心脚本

- `scripts/perf-benchmark.ts` - 主基准测试脚本
- `scripts/perf-lsp-e2e.ts` - LSP 端到端测试
- `scripts/perf-assert-report.ts` - 报告验证脚本
- `scripts/perf-utils.ts` - 百分位数统计工具

### 测试支持

- `test/generators.ts` - 项目生成器（Medium/Large）
- `test/benchmark.ts` - 独立编译基准测试
- `test/perf-utils.test.ts` - 统计工具单元测试

### 辅助工具

- `scripts/lsp-client-helper.ts` - LSP stdio 客户端
- `scripts/perf-utils.ts` - 性能统计函数（p50/p95/p99）

## 常见问题

**Q: 为什么 Medium 项目是生成的而非真实代码？**
A: 真实项目规模不可控且依赖外部文件，生成项目确保可重现性和规模可调。

**Q: 为什么 LSP hover 超时不阻塞 CI？**
A: 这是已知问题，将在 Stage 3.1 LSP 架构重构中解决，当前不应影响编译性能验证。

**Q: 如何调整测试规模？**
A: 修改 `scripts/perf-benchmark.ts` 中的 `prepareProjects()` 函数：
```typescript
const mediumProject = generateMediumProject(60, 42);  // 60 模块
const largeProject = generateLargeProgram(100);       // 100 函数
```

**Q: 报告中的 "允许上限" 如何计算？**
A: 使用公式 `baseline × (1 + PERF_REGRESSION_TOLERANCE)`，默认容忍度 20%。

**Q: p50/p95/p99 分别代表什么？**
A:
- **p50**（中位数）：50% 的样本低于此值，代表典型性能
- **p95**：95% 的样本低于此值，代表大部分情况
- **p99**：99% 的样本低于此值，代表极端情况

## 更新记录

- **2025-10-09**: 初始版本，覆盖 Stage 3.2 性能基准系统实施内容
