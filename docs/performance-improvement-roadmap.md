# Aster 性能改进路线图

本文档详细说明 Aster 语言的四个关键性能改进方向，包括实施计划、预期效果和技术细节。

## 概览

| 改进项 | 预期性能提升 | 实施复杂度 | 优先级 | 预计时间 |
|-------|-------------|-----------|--------|---------|
| Native Image 支持 | 启动时间 100x+，内存 50%+ | 高 | P0 | 3-4 周 |
| AOT 编译选项 | 预热时间减少 50% | 中 | P1 | 2-3 周 |
| 标准库函数内联 | 10-20% 整体性能提升 | 低 | P1 | 1-2 周 |
| 并行执行引擎 | 多核场景 2-8x | 高 | P2 | 4-6 周 |

## 1. Native Image 支持

### 目标

将 Aster 程序编译为原生可执行文件，实现：
- ⚡ **极快启动**: <50ms（vs 当前 5-10 秒）
- 💾 **低内存**: <50MB（vs 当前 300-500MB）
- 📦 **独立部署**: 无需 JVM

### 当前状态

```
Aster Source → Core IR → Truffle AST → JIT 编译 → 执行
                                        ↑
                                    需要 JVM
                                    启动慢（5-10s）
                                    内存高（300-500MB）
```

### 目标状态

```
Aster Source → Core IR → Truffle AST → Native Image 编译
                                              ↓
                                        原生可执行文件
                                        ↓
                                    启动快（<50ms）
                                    内存低（<50MB）
                                    无需 JVM
```

### 实施步骤

#### 阶段 1: 基础配置（1 周）

**任务 1.1: 添加 Native Image 插件**

在 `build.gradle.kts` 中添加：

```kotlin
plugins {
    id("org.graalvm.buildtools.native") version "0.10.0"
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("aster")
            mainClass.set("aster.Main")
            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("--initialize-at-build-time=aster")
        }
    }
}
```

**任务 1.2: 创建反射配置**

创建 `src/main/resources/META-INF/native-image/reflect-config.json`:

```json
[
  {
    "name": "aster.truffle.nodes.AsterExpressionNode",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true
  },
  {
    "name": "aster.truffle.runtime.Builtins",
    "allDeclaredMethods": true
  }
]
```

**任务 1.3: 使用 Native Image Agent 自动生成配置**

```bash
# 运行应用并生成配置
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
     -jar aster-truffle/build/libs/aster-truffle.jar \
     test-program.aster

# Agent 将自动生成:
# - reflect-config.json (反射配置)
# - jni-config.json (JNI 配置)
# - proxy-config.json (动态代理配置)
# - resource-config.json (资源文件配置)
```

#### 阶段 2: 解决反射问题（1-2 周）

**挑战**: Truffle 使用反射创建节点

**解决方案**: 使用 `@GenerateNodeFactory` 和编译时代码生成

```java
// 之前：运行时反射
public class AddNode extends AsterExpressionNode {
    // 反射创建 - Native Image 不支持
}

// 之后：编译时生成
@GenerateNodeFactory
public abstract class AddNode extends AsterExpressionNode {
    @Specialization
    public int doInt(int left, int right) {
        return left + right;
    }
}
// 生成 AddNodeGen 类，无需反射
```

**任务 2.1: 重构所有 Truffle 节点使用 DSL**

```bash
# 受影响的文件（估计 20-30 个节点）
aster-truffle/src/main/java/aster/truffle/nodes/
├── expression/
│   ├── AddNode.java       ✅ 已使用 DSL
│   ├── SubNode.java       ❌ 需要重构
│   ├── MulNode.java       ❌ 需要重构
│   └── ...
├── control/
│   ├── IfNode.java        ❌ 需要重构
│   ├── MatchNode.java     ❌ 需要重构
│   └── ...
└── builtin/
    ├── ListMapNode.java   ❌ 需要重构
    └── ...
```

**任务 2.2: 标记初始化时机**

```java
// 在类级别标记可以在编译时初始化的类
@Fold
public class Constants {
    public static final int MAX_INT = Integer.MAX_VALUE;
}

// 在 build.gradle.kts 中指定
buildArgs.add("--initialize-at-build-time=aster.runtime.Constants")
```

#### 阶段 3: 资源和序列化（3-5 天）

**任务 3.1: 资源文件配置**

```json
// resource-config.json
{
  "resources": {
    "includes": [
      {"pattern": ".*\\.json$"},
      {"pattern": ".*\\.aster$"}
    ]
  }
}
```

**任务 3.2: 序列化配置**

```json
// serialization-config.json
[
  {
    "name": "aster.core.IR$FuncDecl"
  },
  {
    "name": "aster.core.IR$CallNode"
  }
]
```

#### 阶段 4: 编译和测试（3-5 天）

**任务 4.1: 首次 Native Image 编译**

```bash
# 编译为原生可执行文件
./gradlew nativeCompile

# 输出: aster-truffle/build/native/nativeCompile/aster
```

**任务 4.2: 测试基本功能**

```bash
# 测试 Hello World
./build/native/nativeCompile/aster hello.aster

# 测试 Fibonacci
./build/native/nativeCompile/aster fibonacci.aster

# 测试标准库
./build/native/nativeCompile/aster stdlib-test.aster
```

**任务 4.3: 性能基准测试**

```bash
# 对比启动时间
time java -jar aster.jar program.aster     # 预期: 5-10 秒
time ./aster program.aster                 # 目标: <50ms

# 对比内存占用
java -Xmx100m -jar aster.jar program.aster # 当前: 300-500MB
./aster program.aster                      # 目标: <50MB
```

#### 阶段 5: 优化和文档（3-5 天）

**任务 5.1: PGO（Profile-Guided Optimization）**

```bash
# 阶段 1: 收集剖析数据
java -Dgraal.PGOInstrument=profile.iprof \
     -jar aster.jar typical-workload.aster

# 阶段 2: 使用剖析数据编译
./gradlew nativeCompile \
  --pgo-instrument=profile.iprof
```

**预期提升**: 额外 20-30% 性能

**任务 5.2: 二进制大小优化**

```kotlin
// build.gradle.kts
buildArgs.add("-O3")                      // 最高优化级别
buildArgs.add("--gc=serial")              // 更小的 GC
buildArgs.add("-H:+StripDebugInfo")       // 去除调试信息
buildArgs.add("-H:-AddAllCharsets")       // 仅包含需要的字符集
```

**预期**: 二进制大小从 ~100MB 减少到 ~30MB

**任务 5.3: 编写用户文档**

创建 `docs/native-image-guide.md`:

```markdown
# Native Image 使用指南

## 编译为原生可执行文件

\`\`\`bash
./gradlew nativeCompile
\`\`\`

## 运行

\`\`\`bash
./build/native/nativeCompile/aster your-program.aster
\`\`\`

## 限制

- ❌ 不支持动态类加载
- ❌ 不支持反射（需提前配置）
- ✅ 支持所有 Aster 语言特性
```

### 预期效果

| 指标 | 当前 (JVM) | Native Image | 提升 |
|-----|-----------|-------------|------|
| 启动时间 | 5-10 秒 | <50ms | **100x+** |
| 内存占用 | 300-500MB | <50MB | **6-10x** |
| 峰值性能 | 100% | 80-90% | -10-20% |
| 二进制大小 | JVM + JAR ~200MB | ~30MB | **6x** |
| 部署复杂度 | 需要 JVM | 单文件 | **简化** |

**权衡**:
- ✅ 极快启动和低内存适合 CLI 工具、Serverless、容器化部署
- ⚠️ 峰值性能略低（无 JIT 优化），适合短期任务
- ⚠️ 编译时间较长（2-5 分钟 vs 秒级）

---

## 2. AOT 编译选项

### 目标

预编译热点函数，减少 JIT 预热时间：
- 🚀 **减少预热时间 50%**: 从 2000-5000 次迭代减少到 1000-2000 次
- ⚡ **更快达到峰值性能**: 从 30-60 秒减少到 10-30 秒
- 📊 **稳定性能**: 减少性能波动

### 当前状态

```
冷启动 → 解释执行 → JIT 分析 → 编译 → 优化 → 峰值性能
         (慢)      (2000-5000 次迭代)      (30-60s)
```

### 目标状态

```
冷启动 → AOT 预编译代码 → JIT 补充优化 → 峰值性能
         (快)              (1000-2000 次)   (10-30s)
```

### 实施步骤

#### 阶段 1: 识别热点函数（3-5 天）

**任务 1.1: 使用 JFR 剖析典型工作负载**

```bash
# 运行剖析
java -XX:StartFlightRecording=duration=60s,filename=hotspots.jfr \
     -jar aster.jar typical-workload.aster

# 分析热点函数
jfr print --events jdk.ExecutionSample hotspots.jfr | \
    grep "aster.truffle" | \
    sort | uniq -c | sort -nr | head -20
```

**预期输出**:
```
热点函数 (执行次数):
1. aster.truffle.nodes.expression.AddNode.doInt - 1,234,567 次
2. aster.truffle.nodes.control.IfNode.execute - 987,654 次
3. aster.truffle.builtins.ListMapNode.execute - 456,789 次
...
```

**任务 1.2: 创建热点函数列表**

```java
// src/main/resources/META-INF/hotspots.txt
aster.truffle.nodes.expression.AddNode
aster.truffle.nodes.expression.SubNode
aster.truffle.nodes.expression.MulNode
aster.truffle.nodes.control.IfNode
aster.truffle.builtins.ListMapNode
aster.truffle.builtins.ListFilterNode
aster.truffle.builtins.ListReduceNode
```

#### 阶段 2: 实现 AOT 编译（1-2 周）

**方案 A: 使用 Truffle AOT**

```kotlin
// build.gradle.kts
tasks.register("aotCompile") {
    doLast {
        exec {
            commandLine(
                "java",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+EnableJVMCI",
                "-XX:+UseJVMCICompiler",
                "-XX:+EagerJVMCI",
                "-Dgraal.CompileImmediately=true",
                "-Dgraal.CompileOnly=aster.truffle",
                "-jar", "aster-truffle.jar"
            )
        }
    }
}
```

**方案 B: 使用 GraalVM Ahead-of-Time 编译**

```kotlin
graalvmNative {
    binaries {
        named("aot") {
            buildArgs.add("-H:CompileImmediately=true")
            buildArgs.add("-H:CompileOnly=aster.truffle.nodes.*")
            buildArgs.add("-H:+AOT")
        }
    }
}
```

**任务 2.1: 实现编译时触发**

```java
// AsterLanguage.java
@TruffleLanguage.Registration(...)
public class AsterLanguage extends TruffleLanguage<AsterContext> {

    @Override
    protected void initializeContext(AsterContext context) {
        // AOT 模式：编译时预编译热点函数
        if (Boolean.getBoolean("aster.aot")) {
            precompileHotspots(context);
        }
    }

    private void precompileHotspots(AsterContext context) {
        // 读取热点函数列表
        List<String> hotspots = loadHotspotsList();

        for (String hotspot : hotspots) {
            // 触发编译
            Truffle.getRuntime().createCallTarget(
                createRootNodeFor(hotspot)
            );
        }
    }
}
```

#### 阶段 3: 集成到构建流程（3-5 天）

**任务 3.1: Gradle 任务集成**

```kotlin
// build.gradle.kts
tasks.register<JavaExec>("buildWithAOT") {
    group = "build"
    description = "Build Aster with AOT-compiled hotspots"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("aster.Main")

    jvmArgs = listOf(
        "-Daster.aot=true",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+UseJVMCICompiler",
        "-Dgraal.CompileImmediately=true"
    )
}
```

**任务 3.2: CI 集成**

```yaml
# .github/workflows/build.yml
- name: Build with AOT
  run: ./gradlew buildWithAOT

- name: Test AOT performance
  run: |
    ./gradlew bench:jit-aot
    # 验证预热时间减少
```

#### 阶段 4: 测试和验证（3-5 天）

**任务 4.1: 预热时间基准测试**

```java
// AOTBenchmark.java
public class AOTBenchmark {

    @Test
    public void compareWarmupTime() {
        // 无 AOT
        long noAotWarmup = measureWarmupTime(false);

        // 有 AOT
        long withAotWarmup = measureWarmupTime(true);

        // 验证提升
        double improvement = (double) noAotWarmup / withAotWarmup;
        assertTrue(improvement >= 1.5, // 至少 1.5x 提升
                   "Expected 50% warmup reduction, got: " + improvement);
    }

    private long measureWarmupTime(boolean enableAot) {
        // 测量达到峰值性能所需时间
        long start = System.nanoTime();
        runUntilStable(enableAot);
        return System.nanoTime() - start;
    }
}
```

**任务 4.2: 性能回归测试**

```bash
# 对比有/无 AOT 的峰值性能
npm run bench:jit          # 无 AOT 基线
npm run bench:jit-aot      # 有 AOT

# 确保峰值性能不降低
```

### 预期效果

| 指标 | 无 AOT | 有 AOT | 提升 |
|-----|-------|-------|------|
| 预热迭代次数 | 2000-5000 | 1000-2000 | **50%** |
| 达到峰值时间 | 30-60s | 10-30s | **50%** |
| 峰值性能 | 100% | 100% | 持平 |
| 启动时间 | 5-10s | 5-10s | 持平 |
| 构建时间 | 30s | 60-90s | +100% ⚠️ |

**适用场景**:
- ✅ 中期运行任务（30 秒 - 5 分钟）
- ✅ 需要快速响应的服务
- ✅ 性能敏感的 API 端点

---

## 3. 标准库函数内联

### 目标

内联热点标准库函数，消除函数调用开销：
- ⚡ **10-20% 性能提升**: 消除频繁调用的开销
- 📦 **更小的调用栈**: 减少栈帧创建
- 🎯 **JIT 友好**: 更容易优化

### 当前状态

```aster
// 用户代码
let result = add(a, b);

// 运行时
→ 查找 "add" 函数
→ 创建栈帧
→ 调用 Builtins.add(a, b)
→ 执行加法
→ 返回结果
→ 销毁栈帧
```

**开销**: 函数调用 + 栈帧管理

### 目标状态

```aster
// 用户代码
let result = add(a, b);

// 编译时内联
→ 直接执行 a + b
```

**开销**: 几乎为零

### 实施步骤

#### 阶段 1: 识别可内联函数（2-3 天）

**标准**:
- ✅ **简单函数**: 代码少于 5 行
- ✅ **无副作用**: 纯函数
- ✅ **高频调用**: JFR 显示调用次数高

**任务 1.1: 分析标准库调用频率**

```bash
# 剖析标准库调用
jfr print --events aster.builtin.Call hotspots.jfr | \
    sort | uniq -c | sort -nr

# 输出:
# 1,234,567  add
# 987,654    sub
# 456,789    mul
# 234,567    List.map
# 123,456    Text.length
```

**任务 1.2: 创建内联候选列表**

```java
// 高优先级内联（调用频率最高）
- add, sub, mul, div, mod           // 算术运算
- eq, ne, lt, le, gt, ge            // 比较运算
- and, or, not                      // 逻辑运算

// 中优先级内联
- Text.length                       // 文本操作
- List.length                       // 列表操作
- Result.unwrap                     // Result 操作

// 低优先级（复杂函数）
- List.map, List.filter             // 高阶函数（不内联）
- Text.split                        // 复杂操作（不内联）
```

#### 阶段 2: 实现内联机制（1 周）

**方案: 使用 Truffle DSL `@Fallback` 和 `@Cached`**

```java
// 之前: 通过 CallTarget 调用
public class CallNode extends AsterExpressionNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        CallTarget target = lookupFunction("add");
        return target.call(args);  // 间接调用
    }
}

// 之后: 编译时识别并内联
@ImportStatic(Builtins.class)
public abstract class CallNode extends AsterExpressionNode {

    @Specialization(guards = "target == '+' || target == 'add'")
    public int inlineAdd(
            String target,
            int left,
            int right) {
        return left + right;  // 直接内联
    }

    @Specialization(guards = "target == '-' || target == 'sub'")
    public int inlineSub(
            String target,
            int left,
            int right) {
        return left - right;  // 直接内联
    }

    // 其他函数通过 CallTarget
    @Fallback
    public Object callGeneric(
            String target,
            Object[] args) {
        CallTarget callTarget = lookupFunction(target);
        return callTarget.call(args);
    }
}
```

**任务 2.1: 重构 CallNode 支持内联**

```java
// aster-truffle/src/main/java/aster/truffle/nodes/CallNode.java
@ImportStatic(InlinableBuiltins.class)
public abstract class CallNode extends AsterExpressionNode {

    @Child private AsterExpressionNode target;
    @Children private final AsterExpressionNode[] args;

    // 内联 add(Int, Int)
    @Specialization(
        guards = "isAdd(targetName)",
        limit = "3"
    )
    public int inlineAddInt(
            @Cached("getTargetName()") String targetName,
            int left,
            int right) {
        return left + right;
    }

    // 内联 add(Long, Long)
    @Specialization(
        guards = "isAdd(targetName)",
        limit = "3"
    )
    public long inlineAddLong(
            @Cached("getTargetName()") String targetName,
            long left,
            long right) {
        return left + right;
    }

    // ... 其他内联特化

    protected boolean isAdd(String name) {
        return "+".equals(name) || "add".equals(name);
    }

    protected String getTargetName() {
        if (target instanceof NameNode) {
            return ((NameNode) target).getName();
        }
        return null;
    }
}
```

**任务 2.2: 创建 InlinableBuiltins 工具类**

```java
// InlinableBuiltins.java
public class InlinableBuiltins {

    // 算术运算
    public static int add(int a, int b) { return a + b; }
    public static long add(long a, long b) { return a + b; }
    public static double add(double a, double b) { return a + b; }

    public static int sub(int a, int b) { return a - b; }
    public static int mul(int a, int b) { return a * b; }
    public static int div(int a, int b) { return a / b; }

    // 比较运算
    public static boolean eq(int a, int b) { return a == b; }
    public static boolean lt(int a, int b) { return a < b; }
    public static boolean le(int a, int b) { return a <= b; }

    // 逻辑运算
    public static boolean and(boolean a, boolean b) { return a && b; }
    public static boolean or(boolean a, boolean b) { return a || b; }
    public static boolean not(boolean a) { return !a; }

    // 文本操作
    public static int textLength(String s) { return s.length(); }

    // 列表操作
    public static int listLength(List<?> list) { return list.size(); }
}
```

#### 阶段 3: 测试和验证（3-5 天）

**任务 3.1: 单元测试**

```java
@Test
public void testInlinedAdd() {
    // 验证内联后行为正确
    Object result = execute("add", 5, 3);
    assertEquals(8, result);
}

@Test
public void testInlinedAddPerformance() {
    // 验证性能提升
    long baseline = benchmark(() -> callViaCallTarget("add", 5, 3));
    long inlined = benchmark(() -> executeInlined("add", 5, 3));

    double improvement = (double) baseline / inlined;
    assertTrue(improvement >= 1.1, // 至少 10% 提升
               "Expected 10% improvement, got: " + improvement);
}
```

**任务 3.2: 基准测试**

```bash
# 运行内联前后对比
npm run bench:jit-no-inline    # 无内联基线
npm run bench:jit-inline       # 有内联

# 预期: 10-20% 性能提升
```

### 预期效果

| 操作类型 | 无内联 (ms/迭代) | 有内联 (ms/迭代) | 提升 |
|---------|----------------|-----------------|------|
| 算术密集 (Fibonacci) | 26.25 | 21-23 | **12-20%** |
| 比较密集 (QuickSort) | 99.37 | 85-92 | **8-14%** |
| 混合操作 | - | - | **10-15%** |

**权衡**:
- ✅ 显著性能提升，几乎无成本
- ✅ 代码更简洁（内联逻辑在编译器中）
- ⚠️ 增加 CallNode 复杂度
- ⚠️ 需要维护内联函数列表

---

## 4. 并行执行引擎

### 目标

自动并行化纯函数，充分利用多核 CPU：
- 🚀 **多核加速 2-8x**: 根据核心数线性扩展
- 🎯 **自动识别**: 无需用户标注
- 🔒 **安全并行**: 仅并行化纯函数

### 当前状态

```aster
// 用户代码
let results = List.map(largeList, expensiveFunction);

// 运行时: 单线程顺序执行
for item in largeList {
    result = expensiveFunction(item);  // 逐个执行
    results.append(result);
}
```

**问题**: 8 核 CPU 仅使用 1 核，浪费 87.5% 计算资源

### 目标状态

```aster
// 用户代码不变
let results = List.map(largeList, expensiveFunction);

// 运行时: 自动并行化
parallel_for item in largeList {
    result = expensiveFunction(item);  // 8 个线程并行执行
    results[index] = result;
}
```

**效果**: 8 核 CPU 全部使用，接近 8x 加速

### 实施步骤

#### 阶段 1: 纯函数分析（1-2 周）

**任务 1.1: 实现纯函数检测器**

```java
// PurityAnalyzer.java
public class PurityAnalyzer {

    public boolean isPure(AsterFunctionNode func) {
        // 检查条件:
        // 1. 无 IO 效果
        if (hasIOEffects(func)) return false;

        // 2. 无全局变量修改
        if (modifiesGlobalState(func)) return false;

        // 3. 无可变数据结构修改
        if (mutatesArguments(func)) return false;

        // 4. 所有调用的函数也是纯函数
        if (!allCallsArePure(func)) return false;

        return true;
    }

    private boolean hasIOEffects(AsterFunctionNode func) {
        // 检查是否有 IO 效果标记
        return func.getEffects().contains(Effect.IO);
    }

    private boolean modifiesGlobalState(AsterFunctionNode func) {
        // 分析 AST，查找全局变量写入
        GlobalStateVisitor visitor = new GlobalStateVisitor();
        func.accept(visitor);
        return visitor.hasGlobalWrites();
    }
}
```

**任务 1.2: 标记纯函数**

```java
// AsterFunctionNode.java
public class AsterFunctionNode extends RootNode {

    @CompilationFinal
    private Boolean isPure;  // 缓存纯度分析结果

    public boolean isPure() {
        if (isPure == null) {
            isPure = PurityAnalyzer.analyze(this);
        }
        return isPure;
    }
}
```

#### 阶段 2: 并行执行引擎（2-3 周）

**任务 2.1: 实现并行 List.map**

```java
// ParallelListMapNode.java
public class ParallelListMapNode extends AsterExpressionNode {

    @Child private AsterExpressionNode listExpr;
    @Child private AsterExpressionNode funcExpr;

    private static final int PARALLEL_THRESHOLD = 100;  // 最小并行阈值
    private static final ForkJoinPool POOL = ForkJoinPool.commonPool();

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        List<?> list = (List<?>) listExpr.executeGeneric(frame);
        CallTarget func = (CallTarget) funcExpr.executeGeneric(frame);

        // 小列表：顺序执行
        if (list.size() < PARALLEL_THRESHOLD) {
            return sequentialMap(list, func);
        }

        // 检查函数纯度
        if (!isPureFunction(func)) {
            return sequentialMap(list, func);  // 不纯，顺序执行
        }

        // 大列表 + 纯函数：并行执行
        return parallelMap(list, func);
    }

    private List<Object> parallelMap(List<?> list, CallTarget func) {
        return POOL.submit(() ->
            list.parallelStream()
                .map(item -> func.call(item))
                .collect(Collectors.toList())
        ).join();
    }

    private List<Object> sequentialMap(List<?> list, CallTarget func) {
        return list.stream()
                   .map(item -> func.call(item))
                   .collect(Collectors.toList());
    }

    private boolean isPureFunction(CallTarget func) {
        RootNode root = ((RootCallTarget) func).getRootNode();
        if (root instanceof AsterFunctionNode) {
            return ((AsterFunctionNode) root).isPure();
        }
        return false;  // 保守策略：未知函数不并行
    }
}
```

**任务 2.2: 实现并行 List.filter**

```java
// ParallelListFilterNode.java
public class ParallelListFilterNode extends AsterExpressionNode {

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        List<?> list = (List<?>) listExpr.executeGeneric(frame);
        CallTarget predicate = (CallTarget) predicateExpr.executeGeneric(frame);

        if (list.size() < PARALLEL_THRESHOLD || !isPureFunction(predicate)) {
            return sequentialFilter(list, predicate);
        }

        return parallelFilter(list, predicate);
    }

    private List<Object> parallelFilter(List<?> list, CallTarget predicate) {
        return POOL.submit(() ->
            list.parallelStream()
                .filter(item -> (Boolean) predicate.call(item))
                .collect(Collectors.toList())
        ).join();
    }
}
```

**任务 2.3: 实现并行 List.reduce**

```java
// 注意: reduce 需要结合律才能安全并行
public class ParallelListReduceNode extends AsterExpressionNode {

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        List<?> list = (List<?>) listExpr.executeGeneric(frame);
        Object initial = initialExpr.executeGeneric(frame);
        CallTarget reducer = (CallTarget) reducerExpr.executeGeneric(frame);

        // reduce 需要结合律，保守策略：不并行
        // 除非用户显式标记 @associative
        if (!isAssociative(reducer)) {
            return sequentialReduce(list, initial, reducer);
        }

        return parallelReduce(list, initial, reducer);
    }

    private Object parallelReduce(List<?> list, Object initial, CallTarget reducer) {
        return POOL.submit(() ->
            list.parallelStream()
                .reduce(initial,
                        (acc, item) -> reducer.call(acc, item),
                        (acc1, acc2) -> reducer.call(acc1, acc2))
        ).join();
    }
}
```

#### 阶段 3: 自动选择并行策略（1 周）

**任务 3.1: 成本模型**

```java
// ParallelCostModel.java
public class ParallelCostModel {

    public boolean shouldParallelize(
            int listSize,
            CallTarget func,
            int availableCores) {

        // 估算顺序执行成本
        long sequentialCost = listSize * estimateFunctionCost(func);

        // 估算并行执行成本
        long parallelCost = (listSize / availableCores) * estimateFunctionCost(func)
                          + THREAD_OVERHEAD * availableCores;

        // 仅当并行更快时才并行
        return parallelCost < sequentialCost;
    }

    private long estimateFunctionCost(CallTarget func) {
        // 简单启发式: 函数复杂度
        // 更精确的方法: 运行时剖析
        return func.getRootNode().getCost().getMinCost();
    }

    private static final long THREAD_OVERHEAD = 1000; // 线程创建/同步开销
}
```

**任务 3.2: 动态调整**

```java
// 运行时监控并行效率
public class ParallelMonitor {

    private static final Map<String, ParallelStats> stats = new ConcurrentHashMap<>();

    public static void recordExecution(
            String functionName,
            int listSize,
            long sequentialTime,
            long parallelTime) {

        stats.computeIfAbsent(functionName, k -> new ParallelStats())
             .record(listSize, sequentialTime, parallelTime);

        // 如果并行不划算，禁用
        if (parallelTime > sequentialTime * 1.2) {
            disableParallelFor(functionName);
        }
    }
}
```

#### 阶段 4: 测试和验证（1-2 周）

**任务 4.1: 正确性测试**

```java
@Test
public void testParallelMapCorrectness() {
    List<Integer> list = IntStream.range(0, 10000)
                                  .boxed()
                                  .collect(Collectors.toList());

    // 顺序执行
    List<Integer> sequential = list.stream()
                                   .map(x -> x * 2)
                                   .collect(Collectors.toList());

    // 并行执行
    List<Integer> parallel = list.parallelStream()
                                 .map(x -> x * 2)
                                 .collect(Collectors.toList());

    // 验证结果一致
    assertEquals(sequential, parallel);
}
```

**任务 4.2: 性能基准测试**

```java
@Test
public void testParallelMapPerformance() {
    List<Integer> list = IntStream.range(0, 100000)
                                  .boxed()
                                  .collect(Collectors.toList());

    // 昂贵的纯函数
    Function<Integer, Integer> expensive = x -> {
        int result = x;
        for (int i = 0; i < 1000; i++) {
            result = (result * 31 + x) % 1000000;
        }
        return result;
    };

    // 顺序执行
    long seqStart = System.nanoTime();
    list.stream().map(expensive).collect(Collectors.toList());
    long seqTime = System.nanoTime() - seqStart;

    // 并行执行
    long parStart = System.nanoTime();
    list.parallelStream().map(expensive).collect(Collectors.toList());
    long parTime = System.nanoTime() - parStart;

    double speedup = (double) seqTime / parTime;
    System.out.println("Speedup: " + speedup + "x");

    // 在 8 核机器上，预期 4-6x 加速
    assertTrue(speedup >= 2.0, "Expected at least 2x speedup");
}
```

### 预期效果

| 场景 | 顺序执行 (ms) | 并行执行 (ms) | 加速比 |
|------|-------------|-------------|--------|
| List.map (10万元素，简单函数) | 100 | 100 | 1x (开销抵消) |
| List.map (10万元素，昂贵函数) | 10000 | 1500 | **6.7x (8核)** |
| List.filter (100万元素) | 2000 | 350 | **5.7x (8核)** |
| 混合操作链 | 15000 | 2500 | **6x (8核)** |

**适用场景**:
- ✅ 大数据集（>1000 元素）
- ✅ 计算密集型函数
- ✅ 纯函数（无副作用）
- ❌ 小数据集（<100 元素，开销大于收益）
- ❌ 有副作用的函数（不安全）

---

## 实施优先级和时间表

### 第一季度（3 个月）

**Q1 Month 1: Native Image 支持（P0）**
- Week 1-2: 基础配置和反射解决
- Week 3-4: 编译测试和优化

**Q1 Month 2: 标准库函数内联（P1）**
- Week 1: 识别热点函数
- Week 2-3: 实现内联机制
- Week 4: 测试和验证

**Q1 Month 3: AOT 编译选项（P1）**
- Week 1: 识别热点函数
- Week 2-3: 实现 AOT 编译
- Week 4: 集成和测试

### 第二季度（3 个月）

**Q2: 并行执行引擎（P2）**
- Month 1: 纯函数分析
- Month 2: 并行执行引擎实现
- Month 3: 优化和生产化

---

## 风险和缓解措施

| 风险 | 影响 | 概率 | 缓解措施 |
|-----|------|------|---------|
| Native Image 反射配置困难 | 高 | 中 | 使用 Native Image Agent 自动生成 |
| AOT 编译增加构建时间 | 中 | 高 | 使用增量编译，仅编译变更 |
| 并行化引入竞态条件 | 高 | 中 | 严格纯函数检测，保守并行策略 |
| 性能提升不及预期 | 中 | 低 | 提前基准测试，设定现实目标 |

---

## 成功指标

### Native Image
- ✅ 启动时间 <50ms
- ✅ 内存占用 <50MB
- ✅ 所有测试通过

### AOT 编译
- ✅ 预热时间减少 50%
- ✅ 峰值性能不降低
- ✅ 构建时间增加 <100%

### 标准库内联
- ✅ 算术密集型任务提升 12-20%
- ✅ 所有测试通过
- ✅ 无性能回归

### 并行执行
- ✅ 8 核场景 6x+ 加速
- ✅ 无竞态条件
- ✅ 自动识别纯函数准确率 >95%

---

## 参考资料

- [GraalVM Native Image Documentation](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Truffle DSL Guide](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/TruffleDSL/)
- [Java Parallel Streams](https://docs.oracle.com/javase/tutorial/collections/streams/parallelism.html)
- [Profile-Guided Optimization](https://www.graalvm.org/latest/reference-manual/java/compiler/)
