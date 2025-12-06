# Aster 语言文档导航

欢迎来到 Aster 语言文档中心！本文档提供从快速入门到生产部署的完整学习路径。

## 🚀 快速开始（选择你的起点）

### 选项 1：Docker 快速体验（5 分钟）
```bash
# 拉取镜像并运行示例
docker pull ghcr.io/aster-cloud/aster-lang:latest
docker run --rm aster-lang aster run examples/fibonacci.aster --func=fibonacci -- 10
```

参考：[Docker 快速启动](../README.md#docker-quick-start)

### 选项 2：本地构建（15 分钟）
```bash
# 克隆并构建
git clone https://github.com/aster-cloud/aster-lang.git
cd aster-lang
npm install && npm run build

# 运行第一个程序
node dist/scripts/cli.js test/cnl/examples/greet.aster
```

参考：[快速入门指南](guide/quickstart.md)

### 选项 3：Truffle Native Image（最佳性能）
```bash
# 使用 GraalVM Native Image 获得最佳启动性能
./gradlew :aster-truffle:nativeCompile
./aster-truffle/build/native/nativeCompile/aster-truffle \
  --func=fibonacci \
  -- examples/fibonacci.aster 10
```

参考：[Truffle 快速入门](guide/truffle-quickstart.md) | [Native Image 构建指南](native-image/build-guide.md)

---

## 📚 学习路径建议

### 🎯 路径 1：1 小时快速上手
**目标**：理解基本语法，运行第一个程序，部署到生产。

1. **快速入门**（15 分钟）
   - [Getting Started Guide](guide/getting-started.md) - 完整的 45-60 分钟新手教程
   - [Language Overview](guide/language-overview.md) - 核心语法概览

2. **运行示例**（15 分钟）
   - [Examples](guide/examples.md) - 常见使用场景示例
   - [Commands Reference](guide/commands.md) - CLI 命令速查

3. **快速部署**（30 分钟）
   - [Truffle Quickstart](guide/truffle-quickstart.md) - GraalVM Truffle 运行时
   - [Native Image 构建](native-image/build-guide.md) - 生成独立可执行文件

**验收标准**：能在 1 小时内构建并部署一个策略程序到生产环境。

---

### 🔍 路径 2：1 天深入理解
**目标**：掌握类型系统、效果系统、模块化和 JVM 互操作。

1. **核心概念**（2 小时）
   - [Language Specification](reference/language-specification.md) - 正式语言规范
   - [Types](reference/types.md) - 类型系统详解（泛型、ADT、Maybe/Result）
   - [Effects & Capabilities](reference/effects-capabilities.md) - 效果系统（∅ ⊑ CPU ⊑ IO[*]）

2. **Stdlib API 参考**（2 小时）
   - [Stdlib API Reference](reference/stdlib-api.md) - 标准库完整 API（77 APIs）
     - 核心类型：Text, List, Map（37 APIs）
     - 高级特性：Result, Maybe, 数值类型（40 APIs）
   - 示例：错误处理链式调用、Maybe vs Null 对比

3. **模块化与互操作**（2 小时）
   - [Modules](reference/modules.md) - 模块系统
   - [JVM Interop](reference/jvm-interop.md) - Java 互操作详解
   - [Interop Overloads](guide/interop-overloads.md) - 重载处理策略

4. **运行时与性能**（2 小时）
   - [Runtime API Reference](reference/runtime-api.md) - Truffle 运行时、JVM 互操作、性能对比
   - [Truffle Architecture](truffle-architecture.md) - 架构设计
   - [Performance Guide](performance-guide.md) - 性能优化建议

**练习**：实现一个带错误处理和 Java 库调用的复杂模块。

---

### 🏗️ 路径 3：3 天生产部署
**目标**：生产级配置、性能优化、故障排查、贡献代码。

#### 第 1 天：生产构建与部署

1. **Native Image 生产构建**（3 小时）
   - [Native Image Build Guide](native-image/build-guide.md) - 完整构建流程
   - [Native Image Limitations](native-image/limitations.md) - 限制与解决方案
   - [Performance Comparison](native-image/performance-comparison.md) - JVM vs Native 性能对比
     - 启动时间：Native Image 快 10x (~50ms vs ~500ms)
     - 内存占用：Native Image 省 6x (~50MB vs ~300MB)
     - 二进制大小：23MB (PGO) / 37MB (baseline)

2. **配置与部署**（3 小时）
   - [Configuration Guide](operations/configuration.md) - 环境变量、配置文件
   - [Deployment Guide](operations/deployment.md) - 容器化部署、多环境管理
   - [Rollback Strategy](operations/rollback.md) - 回滚与灾备

#### 第 2 天：性能优化与监控

3. **性能调优**（4 小时）
   - [Performance Guide](performance-guide.md) - PGO 编译、启动优化
   - [Truffle Performance Benchmarks](truffle-performance-benchmarks.md) - 基准测试结果
   - [Performance Regression Monitoring](performance-regression-monitoring.md) - 回归监控

4. **故障排查**（2 小时）
   - [Troubleshooting Guide](operations/troubleshooting.md) - 常见问题排查
   - [Native Image Troubleshooting](native-image/troubleshooting.md) - Native Image 特定问题

#### 第 3 天：高级特性与贡献

5. **高级特性**（3 小时）
   - [Generics](reference/generics.md) - 泛型系统
   - [Lambdas](reference/lambdas.md) - Lambda 表达式
   - [PII Taint Analysis](reference/pii-taint-analysis.md) - 隐私数据追踪
   - [Effect Inference Algorithm](reference/effect-inference-algorithm.md) - 效果推断（设计文档）

6. **开发者贡献**（3 小时）
   - [Contributing Guide](guide/contributing.md) - 贡献指南
   - [Testing Guide](testing.md) - 测试框架
   - [Architecture Documentation](architecture.md) - 系统架构
   - [Core IR Specification](core-ir-specification.md) - 核心 IR 规范

**交付物**：
- 生产级 Native Image 可执行文件（含 PGO 优化）
- 完整的部署与回滚方案
- 性能基准测试报告

---

## 📖 核心概念

### 语言特性
- [Language Overview](guide/language-overview.md) - 语法概览
- [Language Specification](reference/language-specification.md) - 正式规范
- [Syntax Reference](reference/syntax.md) - 语法详解
- [Types](reference/types.md) - 类型系统
- [Effects & Capabilities](reference/effects-capabilities.md) - 效果系统
- [Generics](reference/generics.md) - 泛型
- [Lambdas](reference/lambdas.md) - Lambda 表达式
- [Modules](reference/modules.md) - 模块系统

### 运行时与后端
- [Truffle Backend](reference/truffle.md) - GraalVM Truffle 后端
- [Truffle Architecture](truffle-architecture.md) - 架构设计
- [ASM Emitter](reference/asm-emitter.md) - JVM 字节码生成器
- [JVM Interop](reference/jvm-interop.md) - Java 互操作

---

## 📋 API 参考

### 标准库 API
- **[Stdlib API Reference](reference/stdlib-api.md)** - 完整标准库 API（1981 行，77 APIs）
  - **核心类型**（37 APIs）：
    - Text：12 个操作（concat, split, trim, substring, etc.）
    - List\<T\>：16 个操作（map, filter, fold, etc.）
    - Map\<K,V\>：9 个操作（get, put, remove, etc.）
  - **高级特性**（40 APIs）：
    - Result\<T,E\>：10 个操作（链式错误处理）
    - Maybe\<T\>：8 个操作（安全空值处理）
    - Int / Long / Double：22 个数值操作

### 运行时 API
- **[Runtime API Reference](reference/runtime-api.md)** - 运行时配置与互操作（756 行）
  - **Truffle 运行时**：CLI 参数、环境变量、性能分析
  - **JVM 互操作**：3 个完整示例、类型映射表、异常处理
  - **Native Image vs JVM**：性能对比、特性差异、配置文件

### 编译器 API（开发者）
- [Compiler API Overview](api/overview.md) - 编译器 API 概览
- [Lexer API](api/lexer.md) - 词法分析器
- [Parser API](api/parser.md) - 语法分析器
- [Canonicalizer API](api/canonicalizer.md) - 规范化
- [Core IR API](api/core.md) - 核心 IR

---

## 🔧 工具与开发

### 编辑器支持
- [LSP Tutorial](guide/lsp-tutorial.md) - Language Server Protocol 教程
- [LSP Code Actions](guide/lsp-code-actions.md) - 代码操作
- [Formatting Guide](guide/formatting.md) - 代码格式化

### CLI 工具
- [Commands Reference](guide/commands.md) - 命令速查
- [Quickstart](guide/quickstart.md) - 快速入门
- [Truffle Quickstart](guide/truffle-quickstart.md) - Truffle 运行时

### 测试与贡献
- [Testing Guide](testing.md) - 测试框架
- [Contributing Guide](guide/contributing.md) - 贡献指南

---

## 🚀 Native Image 部署

### 构建与配置
- [Build Guide](native-image/build-guide.md) - 构建流程
- [Limitations](native-image/limitations.md) - 限制说明
- [Troubleshooting](native-image/troubleshooting.md) - 故障排查
- [Performance Comparison](native-image/performance-comparison.md) - 性能对比

### 性能数据速查

| 指标 | Native Image | JVM (HotSpot) | 优势 |
|------|--------------|---------------|------|
| **启动时间** | ~50ms | ~500ms | **10x 更快** |
| **首次执行** | ~80ms | ~1200ms | **15x 更快** |
| **内存占用** | ~50MB | ~300MB | **6x 更少** |
| **二进制大小** | 23MB (PGO) | N/A | 独立部署 |

参考：[Runtime API - Performance Comparison](reference/runtime-api.md#3-native-image-vs-jvm-模式)

---

## 📊 性能与优化

- [Performance Guide](performance-guide.md) - 性能优化总指南
- [Performance Improvement Roadmap](performance-improvement-roadmap.md) - 优化路线图
- [Performance Regression Monitoring](performance-regression-monitoring.md) - 回归监控
- [Truffle Performance Benchmarks](truffle-performance-benchmarks.md) - Truffle 基准测试
- [Cross-Backend Benchmark Results](cross-backend-benchmark-results.md) - 跨后端对比

---

## 🏗️ 运维部署

- [Configuration](operations/configuration.md) - 配置管理
- [Deployment](operations/deployment.md) - 部署策略
- [Rollback](operations/rollback.md) - 回滚方案
- [Troubleshooting](operations/troubleshooting.md) - 故障排查
- [Operations Overview](operations.md) - 运维总览

---

## 🔬 高级主题

### 类型系统与分析
- [PII Taint Analysis](reference/pii-taint-analysis.md) - 隐私数据追踪
- [Effect Inference Algorithm](reference/effect-inference-algorithm.md) - 效果推断算法（设计文档）

### 后端与编译
- [Core IR Specification](core-ir-specification.md) - 核心 IR 规范
- [ASM Emitter](reference/asm-emitter.md) - JVM 字节码生成
- [Truffle Backend Limitations](truffle-backend-limitations.md) - Truffle 后端限制

### 架构与设计
- [Architecture](architecture.md) - 系统架构
- [Truffle Architecture](truffle-architecture.md) - Truffle 架构
- [Migration Guide](migration-guide.md) - 迁移指南

### 架构决策记录 (ADR)
- [ADR-001: 延迟 Effect Inference 至 Phase 1](decisions/ADR-001-defer-effect-inference.md) - 效果推断延迟决策

---

## 🔍 文档索引（按类型）

### 入门指南（Guides）
```
guide/
├── getting-started.md        - 完整新手教程（45-60 分钟）
├── quickstart.md             - 快速入门
├── truffle-quickstart.md     - Truffle 运行时快速入门
├── language-overview.md      - 语言概览
├── examples.md               - 示例代码
├── commands.md               - CLI 命令参考
├── formatting.md             - 代码格式化
├── lsp-tutorial.md           - LSP 教程
├── lsp-code-actions.md       - LSP 代码操作
├── interop-overloads.md      - Java 互操作重载
├── capabilities.md           - 能力系统
└── contributing.md           - 贡献指南
```

### 语言参考（Reference）
```
reference/
├── language-specification.md - 正式语言规范
├── syntax.md                 - 语法参考
├── types.md                  - 类型系统
├── effects-capabilities.md   - 效果与能力系统
├── effects.md                - 效果系统详解
├── generics.md               - 泛型
├── lambdas.md                - Lambda 表达式
├── modules.md                - 模块系统
├── stdlib-api.md             - 标准库 API（77 APIs，1981 行）⭐
├── runtime-api.md            - 运行时 API（756 行）⭐
├── jvm-interop.md            - JVM 互操作
├── truffle.md                - Truffle 后端
├── asm-emitter.md            - ASM 字节码生成
├── native.md                 - Native 编译
├── production-builds.md      - 生产构建
├── pii-taint-analysis.md     - PII 污点分析
└── effect-inference-algorithm.md - 效果推断算法（设计文档）
```

### Native Image 部署
```
native-image/
├── README.md                 - Native Image 总览
├── build-guide.md            - 构建指南
├── performance-comparison.md - 性能对比
├── limitations.md            - 限制说明
└── troubleshooting.md        - 故障排查
```

### 运维部署（Operations）
```
operations/
├── configuration.md          - 配置管理
├── deployment.md             - 部署策略
├── rollback.md               - 回滚方案
└── troubleshooting.md        - 故障排查
```

### 编译器 API（Compiler API）
```
api/
├── overview.md               - API 概览
├── lexer.md                  - 词法分析器
├── parser.md                 - 语法分析器
├── canonicalizer.md          - 规范化
├── core.md                   - 核心 IR
└── typedoc/                  - TypeDoc 生成的 API 文档
```

### 架构与设计
```
docs/
├── architecture.md           - 系统架构
├── truffle-architecture.md   - Truffle 架构
├── core-ir-specification.md  - Core IR 规范
├── migration-guide.md        - 迁移指南
├── graalvm-setup-guide.md    - GraalVM 设置
├── java25-compatibility.md   - Java 25 兼容性
└── decisions/
    └── ADR-001-defer-effect-inference.md - Effect Inference 延迟决策
```

### 性能相关
```
docs/
├── performance-guide.md                  - 性能优化指南
├── performance-improvement-roadmap.md    - 优化路线图
├── performance-regression-monitoring.md  - 回归监控
├── truffle-performance-benchmarks.md     - Truffle 基准测试
├── truffle-performance-comparison.md     - Truffle 性能对比
├── cross-backend-benchmark-results.md    - 跨后端对比
└── performance-comparison-charts.md      - 性能对比图表
```

---

## ❓ 常见问题

### 我应该从哪里开始？
- **完全新手**：从 [Getting Started Guide](guide/getting-started.md) 开始（45-60 分钟完整教程）
- **有编程经验**：直接看 [Language Overview](guide/language-overview.md) + [Examples](guide/examples.md)
- **需要快速部署**：使用 [Docker 快速启动](../README.md#docker-quick-start) 或 [Truffle Quickstart](guide/truffle-quickstart.md)

### JVM 模式 vs Native Image 模式如何选择？
- **开发环境**：使用 JVM 模式（更好的调试、热重载）
- **生产环境**：
  - CLI 工具、脚本、边缘计算 → **Native Image**（启动快 10x，内存省 6x）
  - 长时间运行的服务 → **JVM**（峰值性能更高）

参考：[Runtime API - Mode Selection](reference/runtime-api.md#模式选择决策)

### 如何查找 Stdlib API？
所有 77 个标准库 API 集中在 [Stdlib API Reference](reference/stdlib-api.md)，包括：
- Text, List, Map 基础操作（37 APIs）
- Result, Maybe 错误处理（18 APIs）
- Int, Long, Double 数值操作（22 APIs）

每个 API 都包含：签名、参数、返回值、效果标注、示例、边界情况说明。

### 如何与 Java 代码互操作？
参考：
1. [JVM Interop Guide](reference/jvm-interop.md) - 详细说明
2. [Runtime API - JVM Interop](reference/runtime-api.md#2-jvm-互操作) - 3 个完整示例
3. [Interop Overloads](guide/interop-overloads.md) - 处理重载方法

### 性能优化建议？
1. 生产构建使用 PGO 编译（23MB vs 37MB）
2. 参考 [Performance Guide](performance-guide.md) 的优化建议
3. 查看 [Native Image Performance Comparison](native-image/performance-comparison.md) 的基准数据

---

## 📞 获取帮助

- **GitHub Issues**: [https://github.com/aster-cloud/aster-lang/issues](https://github.com/aster-cloud/aster-lang/issues)
- **贡献指南**: [Contributing Guide](guide/contributing.md)
- **故障排查**: [Troubleshooting](operations/troubleshooting.md) | [Native Image Troubleshooting](native-image/troubleshooting.md)

---

## 🗺️ 相关资源

- [GitHub 仓库](https://github.com/aster-cloud/aster-lang)
- [GraalVM 官方文档](https://www.graalvm.org/latest/docs/)
- [Truffle 框架文档](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/)
- [Docker 镜像](https://github.com/aster-cloud/aster-lang/pkgs/container/aster-lang)

---

**版本**: Phase 0 Documentation (2025-11-08)
**维护**: Aster Language Team
