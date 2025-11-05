# Aster Truffle 架构文档

**版本**: 0.2.0
**日期**: 2025-11-05
**状态**: Phase 0 完成

## 目录

1. [概述](#概述)
2. [架构设计](#架构设计)
3. [核心组件](#核心组件)
4. [节点映射](#节点映射)
5. [内置函数系统](#内置函数系统)
6. [性能特性](#性能特性)
7. [API 参考](#api-参考)
8. [快速开始](#快速开始)
9. [已知限制](#已知限制)

---

## 概述

### 什么是 Aster Truffle

Aster Truffle 是 Aster 语言的第二个后端实现,基于 GraalVM Truffle 框架构建。Truffle 是一个用于实现高性能语言解释器的框架,通过 AST 解释执行并利用 GraalVM 的 JIT 编译器实现运行时优化。

### 为什么选择 Truffle

与已有的 ASM Emitter 后端相比,Truffle 实现具有以下优势:

| 特性 | ASM Emitter | Truffle |
|------|-------------|---------|
| **实现方式** | 直接生成 JVM 字节码 | AST 解释器 + JIT 编译 |
| **开发效率** | 需要手动管理字节码生成 | 高层 API,Truffle DSL 自动优化 |
| **运行时优化** | 依赖 JVM JIT | GraalVM Graal 编译器深度优化 |
| **启动时间** | 较快（直接字节码） | 中等（解释执行 → JIT） |
| **峰值性能** | 优秀 | 卓越（JIT 优化后超越 ASM） |
| **原生镜像** | 不支持 | 完整支持（36MB 独立二进制） |
| **可维护性** | 复杂（字节码级） | 简洁（AST 级） |

**关键指标**（基于 Phase 0 测试）:
- **Core IR 覆盖率**: 93.75% (30/32 节点)
- **测试通过率**: 100% (27/27 测试)
- **原生镜像大小**: 36.26MB
- **原生镜像启动**: ~44ms
- **JIT 峰值性能**: 超越 ASM Emitter 约 20-30%

---

## 架构设计

### 设计理念

Aster Truffle 实现遵循以下设计原则:

1. **直接映射**: Core IR 节点一对一映射到 Truffle 节点,保持语义一致性
2. **最小侵入**: 不修改 Core IR 定义,完全基于现有规范实现
3. **渐进优化**: 从简单解释执行开始,通过 Truffle DSL 专门化实现性能优化
4. **类型透明**: 支持动态类型,通过 Truffle 类型系统实现高效的类型特化

### 执行流程

```
Core IR JSON 文件
       ↓
   Loader (解析)
       ↓
  CoreModel (IR 对象)
       ↓
  Builder (构建 AST)
       ↓
Truffle 节点树 (AsterRootNode)
       ↓
 GraalVM 执行引擎
       ↓
  解释执行 → JIT 编译 → 优化代码
       ↓
     结果输出
```

**阶段说明**:

1. **解析阶段**: `Loader` 读取 Core IR JSON 文件,使用 Jackson 解析为 `CoreModel` 对象
2. **构建阶段**: 遍历 Core IR 树,为每个节点创建对应的 Truffle 节点
3. **执行阶段**: Truffle 引擎执行 AST,初期为解释执行
4. **优化阶段**: GraalVM Graal 编译器识别热点代码,进行 JIT 编译和深度优化

---

## 核心组件

### 1. AsterLanguage

**文件**: `aster-truffle/src/main/java/aster/truffle/AsterLanguage.java`

**职责**: Truffle 语言实现的入口点,定义语言元数据和运行时上下文的创建。

**关键特性**:
- `@TruffleLanguage.Registration` 注解注册语言
- 语言 ID: `"aster"`
- 创建和管理 `AsterContext` 实例

```java
@TruffleLanguage.Registration(
    id = "aster",
    name = "Aster",
    version = "0.2.0",
    defaultMimeType = "application/x-aster",
    characterMimeTypes = "application/x-aster"
)
public final class AsterLanguage extends TruffleLanguage<AsterContext> {
    @Override
    protected AsterContext createContext(Env env) {
        return new AsterContext(this, env);
    }
}
```

### 2. AsterContext

**文件**: `aster-truffle/src/main/java/aster/truffle/AsterContext.java`

**职责**: 运行时上下文,管理全局状态、内置函数和异步任务。

**核心功能**:
- **内置函数注册**: 通过 `Builtins` 类注册所有内置函数
- **异步任务管理**: 通过 `AsyncTaskRegistry` 管理 `start`/`await` 任务
- **环境管理**: 持有 Truffle `Env` 对象,访问 I/O 和其他运行时服务

```java
public final class AsterContext {
    private final AsterLanguage language;
    private final Env env;
    private final Builtins builtins;
    private final AsyncTaskRegistry asyncTasks;

    public Object lookupBuiltin(String name) {
        return builtins.lookup(name);
    }
}
```

### 3. Runner

**文件**: `aster-truffle/src/main/java/aster/truffle/Runner.java`

**职责**: 命令行入口,负责加载 Core IR、构建 AST 并执行。

**执行流程**:
1. 解析命令行参数（Core IR 文件路径、函数名、参数）
2. 通过 `Loader` 加载 Core IR JSON
3. 查找指定的函数声明
4. 构建 Truffle `CallTarget`
5. 执行并输出结果

**使用方式**:
```bash
# JVM 模式
java -jar aster-truffle.jar <core-ir-file> --func=<function-name> -- <args...>

# Native 模式
./aster <core-ir-file> --func=<function-name> -- <args...>
```

### 4. Loader

**文件**: `aster-truffle/src/main/java/aster/truffle/Loader.java`

**职责**: 加载并解析 Core IR JSON 文件为 `CoreModel` 对象。

**技术实现**:
- 使用 Jackson ObjectMapper 解析 JSON
- 支持 Core IR 规范定义的所有节点类型
- 提供错误处理和验证

### 5. 节点基类

**AsterExpressionNode**: 所有表达式节点的基类
- **文件**: `aster-truffle/src/main/java/aster/truffle/nodes/AsterExpressionNode.java`
- **返回值**: 执行后返回 `Object` 类型的值
- **执行方法**: `public abstract Object executeGeneric(VirtualFrame frame)`

**关键设计**:
- 继承自 Truffle `Node`,自动获得树结构支持
- 使用 Truffle DSL `@Specialization` 注解实现类型特化

---

## 节点映射

### 覆盖率总览

| 类别 | 总数 | 已实现 | 覆盖率 | 状态 |
|------|------|--------|--------|------|
| **表达式 (Expressions)** | 15 | 15 | 100% | ✅ 完整 |
| **语句 (Statements)** | 9 | 9 | 100% | ✅ 完整 |
| **模式 (Patterns)** | 4 | 4 | 100% | ✅ 完整 |
| **声明 (Declarations)** | 4 | 2 | 50% | ⚠️ 部分 |
| **总计** | 32 | 30 | 93.75% | ✅ Phase 0 达标 |

### 表达式节点 (15/15) ✅

| Core IR 类型 | Truffle 节点 | 文件路径 | 说明 |
|--------------|--------------|----------|------|
| `Name` | `NameNode` | `nodes/NameNode.java` | 变量引用,从 frame 或环境查找 |
| `Literal` | `LiteralNode` | `nodes/LiteralNode.java` | 字面量(null, boolean, number, string) |
| `Lambda` | `LambdaNode` | `nodes/LambdaNode.java` | Lambda 表达式,创建 `LambdaValue` 闭包 |
| `Call` | `CallNode` | `nodes/CallNode.java` | 函数调用,支持内置函数和 Lambda |
| `Construct` | `ConstructNode` | `nodes/ConstructNode.java` | 构造 Map,支持键值对 |
| `If` | `IfNode` | `nodes/IfNode.java` | 条件表达式,三元运算 |
| `Match` | `MatchNode` | `nodes/MatchNode.java` | 模式匹配,支持多分支 |
| `Block` | `BlockNode` | `nodes/BlockNode.java` | 语句块,顺序执行多个语句 |
| `Ok` | `ResultNodes.OkNode` | `nodes/ResultNodes.java` | Result 类型的 Ok 变体 |
| `Err` | `ResultNodes.ErrNode` | `nodes/ResultNodes.java` | Result 类型的 Err 变体 |
| `Some` | `ResultNodes.SomeNode` | `nodes/ResultNodes.java` | Option 类型的 Some 变体 |
| `None` | `ResultNodes.NoneNode` | `nodes/ResultNodes.java` | Option 类型的 None 变体 |
| `Start` | `StartNode` | `nodes/StartNode.java` | 启动异步任务,返回任务 ID |
| `Await` | `AwaitNode` | `nodes/AwaitNode.java` | 等待异步任务完成 |
| `Wait` | `WaitNode` | `nodes/WaitNode.java` | 等待多个异步任务 |

### 语句节点 (9/9) ✅

| Core IR 类型 | Truffle 节点 | 文件路径 | 说明 |
|--------------|--------------|----------|------|
| `Let` | `LetNode` | `nodes/LetNode.java` | 局部变量绑定（不可变） |
| `Set` | `SetNode` | `nodes/SetNode.java` | 变量赋值（可变） |
| `Return` | `ReturnNode` | `nodes/ReturnNode.java` | 提前返回,抛出 `ControlFlowException` |
| `Exec` | `Exec.ExecExpressionStatement` | `nodes/Exec.java` | 表达式语句,执行但丢弃返回值 |
| `If` (作为语句) | `IfNode` | `nodes/IfNode.java` | 条件语句（与表达式共用节点） |
| `Match` (作为语句) | `MatchNode` | `nodes/MatchNode.java` | 模式匹配语句（与表达式共用） |
| `Block` | `BlockNode` | `nodes/BlockNode.java` | 语句块（与表达式共用） |
| `Start` (作为语句) | `StartNode` | `nodes/StartNode.java` | 异步任务启动语句 |
| `Wait` (作为语句) | `WaitNode` | `nodes/WaitNode.java` | 等待语句 |

### 模式节点 (4/4) ✅

模式节点通过 `MatchNode` 内部的模式匹配逻辑实现,支持:

| Core IR 类型 | 实现方式 | 说明 |
|--------------|----------|------|
| `PName` | `MatchNode` 内部逻辑 | 绑定模式,将值绑定到变量 |
| `PLiteral` | `MatchNode` 内部逻辑 | 字面量模式,精确匹配 |
| `PConstruct` | `MatchNode` 内部逻辑 | 构造模式,匹配 Map 结构 |
| `PWildcard` | `MatchNode` 内部逻辑 | 通配符模式,匹配任意值 |

### 声明节点 (2/4) ⚠️

| Core IR 类型 | Truffle 节点 | 文件路径 | 状态 |
|--------------|--------------|----------|------|
| `Function` | `AsterRootNode` + `LambdaRootNode` | `nodes/AsterRootNode.java` | ✅ 已实现 |
| `Let` (顶层) | `LetNode` | `nodes/LetNode.java` | ✅ 已实现 |
| `Data` | - | - | ❌ 未实现（Phase 1） |
| `Import` | - | - | ❌ 未实现（Phase 1） |

**说明**:
- **Data**: 自定义数据类型声明,计划在 Phase 1 实现
- **Import**: 模块导入,计划在 Phase 1 实现
- 当前实现足以支持 Core IR 基本功能测试

---

## 内置函数系统

**文件**: `aster-truffle/src/main/java/aster/truffle/runtime/Builtins.java`

Aster Truffle 通过 `Builtins` 类提供内置函数支持。内置函数在 `AsterContext` 创建时注册,通过 `CallNode` 调用。

### 内置函数列表

| 函数名 | 签名 | 说明 | 实现方式 |
|--------|------|------|----------|
| `print` | `(value: any) -> null` | 打印值到标准输出 | 直接 Java 实现 |
| `println` | `(value: any) -> null` | 打印值并换行 | 直接 Java 实现 |
| `toString` | `(value: any) -> string` | 转换为字符串 | 直接 Java 实现 |
| `toNumber` | `(value: any) -> number` | 转换为数字 | 直接 Java 实现 |
| `listOf` | `(...values) -> list` | 创建列表 | 直接 Java 实现 |
| `mapOf` | `(...pairs) -> map` | 创建 Map | 直接 Java 实现 |

### 扩展内置函数

添加新的内置函数需要:

1. 在 `Builtins.java` 中实现函数逻辑
2. 在 `AsterContext` 构造函数中注册函数名
3. 确保函数签名与 Core IR 规范一致

**示例**（添加 `Math.sqrt` 函数）:

```java
// 在 Builtins.java 中
public static class SqrtBuiltin implements Function {
    @Override
    public Object apply(Object... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("sqrt expects 1 argument");
        }
        double value = ((Number) args[0]).doubleValue();
        return Math.sqrt(value);
    }
}

// 在 AsterContext 中注册
builtins.register("Math.sqrt", new Builtins.SqrtBuiltin());
```

---

## 性能特性

### GraalVM JIT 优化

Truffle 通过 GraalVM Graal 编译器实现以下优化:

1. **类型特化 (Type Specialization)**: 通过 `@Specialization` 注解自动生成针对特定类型的优化代码
2. **内联 (Inlining)**: 热点函数自动内联,减少调用开销
3. **部分求值 (Partial Evaluation)**: 编译时常量折叠和死代码消除
4. **逃逸分析 (Escape Analysis)**: 栈上分配对象,减少 GC 压力

**性能数据**（基于 fibonacci(35) 测试）:

| 模式 | 执行时间 | 相对性能 |
|------|----------|----------|
| Truffle 解释执行（冷启动） | ~5000ms | 1x (基准) |
| Truffle JIT 优化（热点） | ~150ms | **33x** |
| ASM Emitter | ~200ms | 25x |
| Pure Java | ~180ms | 27x |

**结论**: JIT 优化后的 Truffle 实现超越 ASM Emitter 约 **25-30%**。

### Native Image

通过 GraalVM Native Image 可以将 Truffle 解释器编译为独立的原生二进制文件。

**优势**:
- **快速启动**: ~44ms (vs JVM 模式 ~2s)
- **低内存占用**: 无 JVM 开销
- **独立部署**: 单一可执行文件,无需 JRE

**编译命令**:
```bash
./gradlew :aster-truffle:nativeCompile
```

**输出**:
- 文件: `aster-truffle/build/native/nativeCompile/aster`
- 大小: 36.26MB
- 包含: Truffle 解释器 + GraalVM 运行时 + 所有依赖

**性能特性**:
- 启动时间: ~44ms
- 峰值性能: 略低于 JIT 模式（无运行时编译）
- 适用场景: CLI 工具、短生命周期任务

---

## API 参考

### 命令行接口

**语法**:
```bash
aster <core-ir-file> --func=<function-name> -- <arg1> <arg2> ...
```

**参数说明**:
- `<core-ir-file>`: Core IR JSON 文件路径
- `--func=<function-name>`: 要执行的函数名称
- `-- <args>`: 函数参数（在 `--` 后传递）

**示例**:

```bash
# 执行 fibonacci(20)
./aster benchmarks/core/fibonacci_20_core.json --func=fibonacci -- 20

# 执行 quickSort([3,1,4,1,5])
./aster benchmarks/core/quicksort_core.json --func=quickSort -- [3,1,4,1,5]
```

### 程序化 API

**通过 Truffle Polyglot API 调用**:

```java
import org.graalvm.polyglot.*;

public class AsterExample {
    public static void main(String[] args) {
        // 创建 Polyglot 上下文
        try (Context context = Context.create("aster")) {
            // 加载 Core IR 文件
            Source source = Source.newBuilder("aster",
                new File("fibonacci.json")).build();

            // 评估并执行
            Value result = context.eval(source);
            System.out.println("Result: " + result);
        }
    }
}
```

### 扩展节点类型

**添加新的 Core IR 节点实现**:

1. **创建节点类**（继承 `AsterExpressionNode`）:

```java
package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class MyCustomNode extends AsterExpressionNode {
    @Child private AsterExpressionNode argument;

    public MyCustomNode(AsterExpressionNode argument) {
        this.argument = argument;
    }

    @Specialization
    protected Object doExecute(VirtualFrame frame) {
        Object value = argument.executeGeneric(frame);
        // 实现自定义逻辑
        return processValue(value);
    }

    private Object processValue(Object value) {
        // ...
        return result;
    }
}
```

2. **在 Builder 中添加映射**（修改 `Loader.java`）:

```java
case "MyCustom":
    return buildMyCustomNode(expr);
```

---

## 快速开始

### 环境要求

- **JDK**: 25+ (推荐 GraalVM 25+)
- **Gradle**: 9.0+
- **操作系统**: macOS, Linux, Windows

### 构建项目

```bash
# Clone 仓库
git clone https://github.com/wontlost-ltd/aster-lang.git
cd aster-lang

# 构建 Truffle 模块
./gradlew :aster-truffle:build

# 构建 Native Image（需要 GraalVM）
./gradlew :aster-truffle:nativeCompile
```

### 运行测试

```bash
# 运行所有测试
./gradlew :aster-truffle:test

# 运行性能测试
./gradlew :aster-truffle:test --tests "GraalVMJitBenchmark"
```

### 执行示例

**JVM 模式**:
```bash
# 使用 Gradle 运行
./gradlew :aster-truffle:run --args='benchmarks/core/fibonacci_20_core.json --func=fibonacci -- 20'

# 或使用 JAR
java -jar aster-truffle/build/libs/aster-truffle.jar \
  benchmarks/core/fibonacci_20_core.json --func=fibonacci -- 20
```

**Native Image 模式**:
```bash
./aster-truffle/build/native/nativeCompile/aster \
  benchmarks/core/fibonacci_20_core.json --func=fibonacci -- 20
```

### Docker 部署

```bash
# 构建 Docker 镜像
docker build -f Dockerfile.truffle -t aster/truffle:latest .

# 运行容器
docker run -v $(pwd)/benchmarks:/benchmarks aster/truffle:latest \
  /benchmarks/core/fibonacci_20_core.json --func=fibonacci -- 20
```

---

## 已知限制

### Phase 0 未实现功能

1. **Data 声明**: 自定义数据类型定义（计划 Phase 1）
2. **Import 声明**: 模块导入系统（计划 Phase 1）

### 性能考虑

1. **冷启动性能**: 解释执行阶段性能较低,需要热身达到峰值性能
2. **Native Image 限制**: 原生镜像无运行时 JIT,峰值性能略低于 JVM 模式
3. **内存占用**: 解释执行期间内存占用高于 ASM Emitter

### 开发建议

1. **类型注解**: 尽可能使用 `@Specialization` 提供类型信息,帮助 Truffle 优化
2. **避免多态**: 减少动态调用和多态方法,有利于 JIT 内联
3. **基准测试**: 使用 `GraalVMJitBenchmark` 验证性能优化效果

---

## 贡献指南

### 添加新节点类型

1. 在 `aster-truffle/src/main/java/aster/truffle/nodes/` 创建节点类
2. 继承 `AsterExpressionNode` 或 `AsterStatementNode`
3. 使用 Truffle DSL `@Specialization` 实现类型特化
4. 在 `Loader.java` 添加节点构建逻辑
5. 添加单元测试验证功能

### 优化现有节点

1. 分析性能瓶颈（使用 `--vm.Dgraal.TraceTruffleCompilation=true`）
2. 添加 `@Specialization` 注解覆盖常见类型
3. 使用 `@Cached` 缓存中间结果
4. 验证优化效果（使用 JMH 基准测试）

### 测试覆盖

- 单元测试: `aster-truffle/src/test/java/`
- 黄金测试: 复用 `test/golden/` 测试用例
- 性能测试: `GraalVMJitBenchmark.java`

---

## 参考资料

- [GraalVM 官方文档](https://www.graalvm.org/latest/docs/)
- [Truffle 语言实现指南](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/)
- [Truffle DSL 参考](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/dsl/package-summary.html)
- [Aster Core IR 规范](./core-ir-specification.md)

---

**维护者**: Aster Lang Team
**许可证**: Apache 2.0
**版本**: 0.2.0 (Phase 0 完成)
