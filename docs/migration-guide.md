# Migration Guide: TypeScript/Java to Truffle Backend

本文档提供从 TypeScript 解释器或 Pure Java 字节码后端迁移到 Truffle 后端的完整指南。

## 目录

1. [为什么迁移到 Truffle](#为什么迁移到-truffle)
2. [架构差异](#架构差异)
3. [逐步迁移路径](#逐步迁移路径)
4. [代码示例对比](#代码示例对比)
5. [性能优化指南](#性能优化指南)
6. [常见问题](#常见问题)

## 为什么迁移到 Truffle

### Truffle 后端的优势

#### 1. 性能提升（使用 GraalVM JIT）

| 场景 | 性能提升 |
|-----|---------|
| 长期运行服务（>1分钟） | **5-10x faster** |
| 计算密集型任务 | **6-8x faster** |
| 递归算法 | **5-7x faster** |

#### 2. 更好的开发体验

✅ **调试能力**:
- Chrome DevTools 协议支持
- 断点调试
- 实时变量查看
- 调用栈追踪

✅ **错误诊断**:
- 更清晰的错误消息
- 准确的行号和位置信息
- 详细的堆栈跟踪

✅ **代码覆盖率**:
- 内置代码覆盖率工具
- 详细的分支覆盖分析

#### 3. 多语言互操作

```java
// 在 Truffle 中调用 JavaScript
Context context = Context.newBuilder("aster", "js")
    .allowAllAccess(true)
    .build();

Value jsFunction = context.eval("js", "function add(a, b) { return a + b; }");
Value result = jsFunction.execute(5, 3);  // 调用 JS 函数
```

#### 4. 未来扩展性

- Native Image 编译（极快启动，低内存占用）
- LLVM 工具链集成
- Profile-Guided Optimization
- 自动并行化

### 何时不应迁移

❌ **以下场景不推荐 Truffle**:

1. **极短期任务** (<5 秒)
   - Pure Java 字节码启动更快
   - Truffle 预热开销无法摊销

2. **内存受限环境** (<512MB 堆)
   - Truffle + JIT 编译器需要额外内存
   - TypeScript 解释器或 Pure Java 更合适

3. **简单脚本工具**
   - TypeScript 解释器足够
   - 无需 JIT 优化开销

## 架构差异

### TypeScript 解释器 vs Truffle

#### TypeScript 解释器架构

```
Aster Source Code (.aster)
    ↓ [Parser]
AST (Abstract Syntax Tree)
    ↓ [Evaluator - 递归遍历]
Evaluation Result
```

**特点**:
- ✅ 简单直观
- ✅ 启动快
- ❌ 性能受限（无优化）
- ❌ 调试困难

#### Truffle 架构

```
Aster Source Code (.aster)
    ↓ [Parser]
Surface AST
    ↓ [Core IR Transformation]
Core IR (JSON)
    ↓ [Truffle Node Builder]
Truffle AST (AsterExpressionNode)
    ↓ [Interpreter Execution]
    │ (JIT Profiling)
    ↓ [GraalVM Compiler]
Optimized Machine Code
    ↓
Execution Result
```

**特点**:
- ✅ 高性能（JIT 优化）
- ✅ 优秀的调试支持
- ✅ 多语言互操作
- ⚠️ 需要预热
- ⚠️ 更复杂的实现

### Pure Java Bytecode vs Truffle

#### Pure Java Bytecode 架构

```
Core IR (JSON)
    ↓ [ASM Bytecode Emitter]
Java Bytecode (.class)
    ↓ [JVM Execution]
Execution Result
```

**特点**:
- ✅ 最快的绝对性能
- ✅ 无预热开销
- ❌ 难以调试（字节码级别）
- ❌ 缺乏动态语言特性
- ❌ 不支持热重载

#### Truffle 优势对比

| 特性 | Pure Java | Truffle |
|-----|----------|---------|
| 峰值性能 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 启动时间 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| 调试体验 | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| 动态特性 | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| 互操作性 | ⭐⭐ | ⭐⭐⭐⭐⭐ |

## 逐步迁移路径

### 阶段 1: 环境准备（1 天）

#### 1.1 安装 GraalVM

参见 [GraalVM Setup Guide](./graalvm-setup-guide.md)

```bash
# 验证安装
java -version
# 输出应包含 "Oracle GraalVM"
```

#### 1.2 更新项目依赖

在 `build.gradle.kts` 中确认 Truffle 依赖：

```kotlin
dependencies {
    implementation("org.graalvm.truffle:truffle-api:25.0.0")
    implementation("org.graalvm.truffle:truffle-runtime:25.0.0")
}
```

#### 1.3 配置开发环境

```bash
# 设置环境变量
export GRAALVM_HOME=/path/to/graalvm
export PATH=$GRAALVM_HOME/bin:$PATH

# 验证 Truffle 可用
./gradlew :aster-truffle:test --tests "ExecutionTestSuite"
```

### 阶段 2: 理解 Core IR（1-2 天）

#### 2.1 学习 Core IR 结构

Core IR 是 Aster 的中间表示，所有后端共享：

```json
{
  "name": "example.add",
  "decls": [{
    "kind": "Func",
    "name": "add",
    "params": [
      {"name": "a", "type": {"kind": "TypeName", "name": "Int"}},
      {"name": "b", "type": {"kind": "TypeName", "name": "Int"}}
    ],
    "ret": {"kind": "TypeName", "name": "Int"},
    "body": {
      "kind": "Block",
      "statements": [{
        "kind": "Return",
        "expr": {
          "kind": "Call",
          "target": {"kind": "Name", "name": "+"},
          "args": [
            {"kind": "Name", "name": "a"},
            {"kind": "Name", "name": "b"}
          ]
        }
      }]
    }
  }]
}
```

#### 2.2 查看现有示例

```bash
# 查看基准测试的 Core IR
ls benchmarks/core/*.json

# 重要示例文件
benchmarks/core/fibonacci_20_core.json    # 递归函数
benchmarks/core/factorial_core.json       # 迭代循环
benchmarks/core/list_map_core.json        # 高阶函数
benchmarks/core/result_map_ok_core.json   # Result 类型
```

### 阶段 3: 迁移执行逻辑（3-5 天）

#### 从 TypeScript 迁移

**之前 (TypeScript)**:

```typescript
// TypeScript 解释器实现
function evalCall(node: CallNode, env: Environment): Value {
  const target = evalExpr(node.target, env);
  const args = node.args.map(arg => evalExpr(arg, env));

  if (target === '+') {
    return args[0] + args[1];
  }
  // ...
}
```

**之后 (Truffle)**:

```java
// Truffle 节点实现
@NodeChild("left")
@NodeChild("right")
public abstract class AddNode extends AsterExpressionNode {

    @Specialization
    public int doInt(int left, int right) {
        return left + right;
    }

    @Specialization
    public long doLong(long left, long right) {
        return left + right;
    }

    @Specialization
    public double doDouble(double left, double right) {
        return left + right;
    }
}
```

**关键差异**:
1. **类型特化**: Truffle 使用 `@Specialization` 为不同类型生成优化代码
2. **不可变 AST**: Truffle 节点不可变，便于 JIT 优化
3. **框架调用**: Truffle 框架负责节点执行和剖析

#### 从 Pure Java 迁移

**之前 (Pure Java Bytecode)**:

```java
// Pure Java - 直接生成字节码
public void emitAdd(CallNode node) {
    emitExpr(node.getArgs().get(0));  // 左操作数 -> 栈
    emitExpr(node.getArgs().get(1));  // 右操作数 -> 栈
    mv.visitInsn(IADD);                // ADD 指令
}
```

**之后 (Truffle)**:

```java
// Truffle - 构建 AST 节点
public AsterExpressionNode buildAdd(CallNode node) {
    AsterExpressionNode left = buildExpr(node.getArgs().get(0));
    AsterExpressionNode right = buildExpr(node.getArgs().get(1));
    return AddNodeGen.create(left, right);
}
```

**关键差异**:
1. **构建 AST 而非字节码**: Truffle 构建可执行的 AST
2. **延迟优化**: JIT 编译器在运行时优化
3. **动态类型**: 支持类型特化和多态

### 阶段 4: 实现标准库（5-7 天）

#### 4.1 内置函数映射

将现有标准库映射到 Truffle `Builtins.java`:

```java
public class Builtins {

    // 算术运算
    public static final CallTarget ADD = createBuiltin("add",
        (args) -> {
            int a = (Integer) args[0];
            int b = (Integer) args[1];
            return a + b;
        });

    // 列表操作
    public static final CallTarget LIST_MAP = createBuiltin("List.map",
        (args) -> {
            List<?> list = (List<?>) args[0];
            CallTarget fn = (CallTarget) args[1];
            // 实现 map 逻辑...
        });

    // Result 类型
    public static final CallTarget RESULT_MAP_OK = createBuiltin("Result.mapOk",
        (args) -> {
            Object result = args[0];
            CallTarget fn = (CallTarget) args[1];
            // 实现 mapOk 逻辑...
        });
}
```

#### 4.2 高阶函数支持

**TypeScript 方式**（函数作为值）:

```typescript
function map<T, U>(list: T[], fn: (x: T) => U): U[] {
    return list.map(fn);
}
```

**Truffle 方式**（CallTarget 调用）:

```java
public static Object listMap(List<?> list, CallTarget fn) {
    List<Object> result = new ArrayList<>();
    for (Object item : list) {
        Object mapped = fn.call(item);  // 调用 lambda CallTarget
        result.add(mapped);
    }
    return result;
}
```

### 阶段 5: 测试与验证（3-5 天）

#### 5.1 运行现有测试

```bash
# 运行 Truffle 测试套件
./gradlew :aster-truffle:test

# 运行 Golden 测试（Core IR 验证）
./gradlew :aster-truffle:test --tests "GoldenTestAdapter"

# 运行执行测试
./gradlew :aster-truffle:test --tests "ExecutionTestSuite"
```

#### 5.2 对比三个后端

```bash
# 运行所有基准测试
npm run bench:all

# 对比输出:
# - Pure Java: 最快（无预热）
# - GraalVM JIT: 次快（预热后）
# - Truffle Interpreter: 最慢（但足够快）
```

#### 5.3 性能验证

确保性能符合预期：

| 基准测试 | 预期加速比 (vs Truffle Interpreter) |
|---------|-----------------------------------|
| Fibonacci(20) | 5-7x |
| Factorial(12) | 5-6x |
| QuickSort(100) | 4-5x |
| List.map | 6-8x |

### 阶段 6: 生产部署（2-3 天）

#### 6.1 配置生产环境

```bash
# JVM 参数配置
-Xms2g -Xmx4g                    # 堆大小
-XX:+UseG1GC                     # G1 垃圾收集器
-XX:MaxGCPauseMillis=200         # GC 停顿目标
-Dgraal.TruffleCompilationThreshold=1000  # JIT 阈值
```

#### 6.2 监控与告警

参见 [Performance Regression Monitoring](./performance-regression-monitoring.md)

```bash
# 启用 JFR 监控
-XX:StartFlightRecording=duration=3600s,filename=production.jfr
```

#### 6.3 回滚计划

保留旧后端（TypeScript 或 Pure Java）作为备份：

```bash
# 如果 Truffle 有问题，立即切回
export ASTER_BACKEND=typescript  # 或 pure-java
```

## 代码示例对比

### 示例 1: 简单函数调用

#### TypeScript 实现

```typescript
// eval.ts
function evalCall(call: CallNode, env: Env): Value {
    const fnName = call.target.name;
    const args = call.args.map(arg => evalExpr(arg, env));

    // 查找函数
    const fn = env.get(fnName);
    if (!fn) throw new Error(`Function ${fnName} not found`);

    // 创建新环境并绑定参数
    const newEnv = new Env(env);
    fn.params.forEach((param, i) => {
        newEnv.set(param.name, args[i]);
    });

    // 执行函数体
    return evalBlock(fn.body, newEnv);
}
```

#### Truffle 实现

```java
// CallNode.java
public class CallNode extends AsterExpressionNode {

    @Child private AsterExpressionNode target;
    @Children private final AsterExpressionNode[] args;

    @ExplodeLoop
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // 获取函数（CallTarget）
        Object targetObj = target.executeGeneric(frame);
        if (!(targetObj instanceof CallTarget)) {
            throw new RuntimeException("Not a function");
        }
        CallTarget callTarget = (CallTarget) targetObj;

        // 执行参数表达式
        Object[] argValues = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            argValues[i] = args[i].executeGeneric(frame);
        }

        // 调用函数
        return callTarget.call(argValues);
    }
}
```

**对比**:
- TypeScript: 递归遍历 AST，动态查找
- Truffle: 节点执行，`@ExplodeLoop` 优化循环展开

### 示例 2: Lambda 表达式

#### TypeScript 实现

```typescript
// eval.ts
function evalLambda(lambda: LambdaNode, env: Env): Closure {
    return {
        params: lambda.params,
        body: lambda.body,
        captures: captureEnv(lambda.captures, env)  // 闭包捕获
    };
}

function applyLambda(closure: Closure, args: Value[]): Value {
    const newEnv = new Env(null);
    // 绑定捕获的变量
    for (const [name, value] of closure.captures) {
        newEnv.set(name, value);
    }
    // 绑定参数
    closure.params.forEach((param, i) => {
        newEnv.set(param.name, args[i]);
    });
    return evalBlock(closure.body, newEnv);
}
```

#### Truffle 实现

```java
// LambdaNode.java
public class LambdaNode extends AsterExpressionNode {

    private final RootCallTarget callTarget;
    @Children private final AsterExpressionNode[] captures;

    @Override
    public CallTarget executeGeneric(VirtualFrame frame) {
        // 执行捕获表达式
        Object[] captureValues = new Object[captures.length];
        for (int i = 0; i < captures.length; i++) {
            captureValues[i] = captures[i].executeGeneric(frame);
        }

        // 返回 CallTarget（带捕获值）
        return Truffle.getRuntime().createCallTarget(
            new LambdaRootNode(callTarget, captureValues)
        );
    }
}
```

**对比**:
- TypeScript: 闭包存储为 JavaScript 对象
- Truffle: CallTarget + 捕获值数组，JIT 优化

### 示例 3: 类型检查

#### TypeScript 实现

```typescript
// typecheck.ts
function checkAdd(node: CallNode, env: TypeEnv): Type {
    const left = inferType(node.args[0], env);
    const right = inferType(node.args[1], env);

    if (left.kind !== 'Int' || right.kind !== 'Int') {
        throw new TypeError(`Add requires Int, got ${left.kind} and ${right.kind}`);
    }

    return { kind: 'Int' };
}
```

#### Truffle 实现

```java
// AddNode.java (类型特化)
public abstract class AddNode extends AsterExpressionNode {

    @Specialization
    public int doInt(int left, int right) {
        return left + right;  // Int 特化
    }

    @Specialization
    public long doLong(long left, long right) {
        return left + right;  // Long 特化
    }

    @Specialization
    public double doDouble(double left, double right) {
        return left + right;  // Double 特化
    }

    // 类型不匹配时回退
    @Fallback
    public Object doGeneric(Object left, Object right) {
        throw new RuntimeException("Unsupported types for +");
    }
}
```

**对比**:
- TypeScript: 静态类型检查（编译期）
- Truffle: 类型特化（运行时），JIT 为每种类型生成优化代码

## 性能优化指南

### 优化 1: 使用 `@Specialization` 替代类型检查

**不良实践**:

```java
public Object executeGeneric(VirtualFrame frame) {
    Object value = child.executeGeneric(frame);
    if (value instanceof Integer) {
        return ((Integer) value) + 1;
    } else if (value instanceof Long) {
        return ((Long) value) + 1L;
    } else {
        throw new RuntimeException("Unsupported type");
    }
}
```

**最佳实践**:

```java
@Specialization
public int doInt(int value) {
    return value + 1;
}

@Specialization
public long doLong(long value) {
    return value + 1;
}
```

**性能提升**: 2-5x（消除类型检查开销）

### 优化 2: 使用 `@ExplodeLoop` 展开循环

**不良实践**:

```java
for (int i = 0; i < args.length; i++) {
    argValues[i] = args[i].executeGeneric(frame);
}
```

**最佳实践**:

```java
@ExplodeLoop
for (int i = 0; i < args.length; i++) {
    argValues[i] = args[i].executeGeneric(frame);
}
```

**性能提升**: 10-20%（小循环完全展开）

### 优化 3: 使用 `@Cached` 缓存热点调用

**不良实践**:

```java
public Object executeCall(CallTarget target, Object[] args) {
    return target.call(args);  // 每次都是间接调用
}
```

**最佳实践**:

```java
@Specialization(guards = "target == cachedTarget")
public Object executeCached(CallTarget target, Object[] args,
        @Cached("target") CallTarget cachedTarget,
        @Cached("create(cachedTarget)") DirectCallNode callNode) {
    return callNode.call(args);  // 直接调用，可内联
}
```

**性能提升**: 3-10x（函数调用开销降低）

## 常见问题

### Q1: 迁移需要多长时间？

**A**: 取决于项目规模和现有代码质量：

| 项目规模 | TypeScript → Truffle | Pure Java → Truffle |
|---------|---------------------|---------------------|
| 小型 (<1000 行) | 1-2 周 | 1 周 |
| 中型 (1000-5000 行) | 3-4 周 | 2-3 周 |
| 大型 (>5000 行) | 6-8 周 | 4-6 周 |

### Q2: 能否保留旧后端作为备份？

**A**: 可以且推荐。多后端架构的优势：

```
Core IR (统一中间表示)
    ├─ TypeScript Interpreter (备份)
    ├─ Pure Java Bytecode (生产)
    └─ Truffle (新功能)
```

通过环境变量切换后端：
```bash
export ASTER_BACKEND=truffle  # 或 typescript / pure-java
```

### Q3: Truffle 是否支持我的语言特性？

**A**: 检查清单：

| 特性 | Truffle 支持 | 说明 |
|-----|------------|------|
| 函数调用 | ✅ | CallTarget 机制 |
| Lambda | ✅ | CallTarget + 捕获值 |
| 闭包 | ✅ | 捕获变量存储在 Frame |
| 高阶函数 | ✅ | CallTarget 作为值传递 |
| 模式匹配 | ✅ | MatchNode 实现 |
| 类型推导 | ⚠️ | 运行时类型特化 |
| 泛型 | ⚠️ | 运行时擦除，需手动特化 |
| 宏 | ❌ | 需在 Core IR 阶段处理 |

### Q4: 如何调试 Truffle 代码？

**A**: 多种方法：

#### 方法 1: 使用 Chrome DevTools

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 \
     -jar your-app.jar
```

然后连接 Chrome DevTools（chrome://inspect）。

#### 方法 2: 使用 IntelliJ IDEA

1. 设置断点在 Truffle 节点代码
2. Debug 模式运行测试
3. 单步执行节点

#### 方法 3: 添加日志

```java
@Override
public Object executeGeneric(VirtualFrame frame) {
    System.out.println("Executing node: " + this.getClass().getSimpleName());
    // ... 节点逻辑
}
```

### Q5: Truffle 的内存占用比 TypeScript 高多少？

**A**: 典型增加：

| 后端 | 堆内存 | 说明 |
|-----|-------|------|
| TypeScript | 100-200 MB | V8 引擎 + AST |
| Truffle Interpreter | 150-250 MB | Truffle 框架开销 |
| GraalVM JIT | 300-500 MB | + JIT 编译器缓存 |
| Pure Java | 100-200 MB | 无额外开销 |

**优化建议**:
- 短期任务：使用 TypeScript 或 Pure Java
- 长期服务：使用 GraalVM JIT，内存开销可摊销

## 后续步骤

### 1. 深入学习 Truffle

- [Truffle Language Implementation Framework](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/)
- [Truffle DSL Guide](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/TruffleDSL/)
- [Truffle Tutorials](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Tutorial/)

### 2. 参考 Aster 代码

```bash
# Truffle 节点实现
aster-truffle/src/main/java/aster/truffle/nodes/

# 标准库实现
aster-truffle/src/main/java/aster/truffle/runtime/Builtins.java

# 测试用例
aster-truffle/src/test/java/aster/truffle/
```

### 3. 性能测试

```bash
# 运行基准测试
npm run bench:all

# 对比三个后端性能
npm run bench:truffle
npm run bench:java
npm run bench:jit
```

### 4. 加入社区

- [GraalVM Slack](https://www.graalvm.org/community/)
- [Truffle GitHub Discussions](https://github.com/oracle/graal/discussions)

## 相关文档

- [GraalVM Setup Guide](./graalvm-setup-guide.md)
- [Performance Comparison Charts](./performance-comparison-charts.md)
- [Performance Regression Monitoring](./performance-regression-monitoring.md)
- [Truffle Performance Comparison](./truffle-performance-comparison.md)
