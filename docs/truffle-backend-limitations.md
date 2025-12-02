# Truffle 后端限制说明

## 概述

Aster 语言的 Truffle 后端（`aster-truffle`）是基于 GraalVM Truffle 框架实现的高性能解释器，专注于 JIT 编译优化和原生性能。由于 Truffle AST interpreter 的架构特性，某些语言特性在该后端中不可用或受限。

## 异步操作限制

### 不支持的特性

Truffle 后端**不支持**以下异步操作：

1. **Await 表达式**
   ```aster
   Let result = Await someAsyncFunc()  // ❌ 不支持
   ```

2. **Start 语句**（启动异步任务）
   ```aster
   Start task as async load()  // ❌ 不支持
   ```

3. **Wait 语句**（等待异步任务）
   ```aster
   Wait for task  // ❌ 不支持
   ```

### 错误信息

尝试执行包含异步操作的代码时，Truffle 后端会抛出 `UnsupportedOperationException`：

```
java.lang.UnsupportedOperationException: 异步操作 (await/start/wait) 在 Truffle 后端尚未支持。
请使用 Java 或 TypeScript 后端运行异步代码。
```

### 技术原因

#### 1. AST Interpreter 架构限制

Truffle 使用 AST interpreter，节点的 `execute()` 方法是同步的：

```java
public Object execute(VirtualFrame frame) {
  // 必须同步返回结果，无法暂停/恢复
}
```

阻塞等待异步结果会：
- 破坏 Truffle 的执行模型
- 触发 JIT 去优化（deoptimization）
- 严重影响性能

#### 2. Continuation 支持限制

GraalVM Truffle 提供的 Continuation 支持（类似协程）仅在 **Bytecode DSL** 中可用：
- aster-truffle 使用 **AST interpreter**，不是 Bytecode DSL
- 重构为 Bytecode DSL 工作量巨大（需要完全重写）

#### 3. Virtual Threads 不支持

Java 21+ 的虚拟线程在 native-image + JIT 模式下不可用：

```
UnsupportedOperationException: Virtual threads are not supported together
with Truffle JIT compilation.
```

### 替代方案

如果你的 Aster 代码需要异步操作，请使用以下后端：

#### ✅ Java 后端（推荐用于生产）

使用 `aster-runtime` 的 CompletableFuture 实现：

```bash
ASTER_COMPILER=java ./aster-lang-cli/build/install/aster-lang-cli/bin/aster-lang-cli compile <file.json>
```

**特点**：
- 完整支持 async/await 语义
- 使用线程池实现真正的并发
- 适合 I/O 密集型任务

#### ✅ TypeScript 后端（推荐用于开发）

使用 JavaScript 原生 async/await：

```bash
ASTER_COMPILER=typescript ./aster-lang-cli/build/install/aster-lang-cli/bin/aster-lang-cli compile <file.json>
```

**特点**：
- 完整支持 async/await 语义
- 使用 event loop 实现非阻塞 I/O
- 快速原型开发

### Truffle 后端的优势

虽然 Truffle 后端不支持异步，但它在以下场景中表现出色：

1. **CPU 密集型计算**
   - JIT 编译优化
   - 接近原生代码性能
   - 适合数值计算、算法实现

2. **性能基准测试**
   - 验证语言核心性能
   - 测试 JIT 优化效果
   - 性能瓶颈分析

3. **原生集成**
   - GraalVM native-image 支持
   - 快速启动时间
   - 低内存占用

### 未来展望

真正支持异步需要以下工作：

1. **重构为 Bytecode DSL**（长期计划）
   - 完全重写解释器架构
   - 使用 Truffle Bytecode DSL API
   - 支持 Continuation 和 yield/resume

2. **等待 Truffle 虚拟线程支持**（依赖上游）
   - GraalVM 需要解除 native-image + JIT 的虚拟线程限制
   - 预计在未来版本中支持

## 总结

| 特性 | Java 后端 | TypeScript 后端 | Truffle 后端 |
|------|----------|----------------|-------------|
| async/await | ✅ 完整支持 | ✅ 完整支持 | ❌ 不支持 |
| CPU 密集计算 | ⭐⭐⭐ | ⭐ | ⭐⭐⭐⭐⭐ |
| I/O 密集任务 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ❌ |
| 启动速度 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 原生镜像 | ⭐⭐⭐ | ❌ | ⭐⭐⭐⭐⭐ |

**建议**：
- 开发阶段使用 TypeScript 后端
- 生产部署根据场景选择 Java（异步）或 Truffle（CPU密集）
- 性能测试使用 Truffle 后端

## 参考资料

- [GraalVM Truffle Documentation](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/)
- [Truffle Safepoint Tutorial](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Safepoint/)
- [Bytecode DSL Continuation API](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/bytecode/ContinuationRootNode.html)
