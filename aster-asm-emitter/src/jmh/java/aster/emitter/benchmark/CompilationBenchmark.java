package aster.emitter.benchmark;

import aster.emitter.CoreModel;
import aster.emitter.Main;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 编译性能基准测试 - 测量从 CoreIR 到 JVM 字节码的编译时间
 *
 * 测试场景：
 * - 小型模块：简单表达式、基础函数（< 10 个声明）
 * - 中型模块：复杂逻辑、多函数调用（10-50 个声明）
 * - 大型模块：实际应用代码（> 50 个声明）
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class CompilationBenchmark {

    private CoreModel.Module smallModule;
    private CoreModel.Module mediumModule;
    private CoreModel.Module largeModule;

    private ObjectMapper mapper;
    private Path tempDir;

    @Setup
    public void setup() throws IOException {
        mapper = new ObjectMapper();
        tempDir = Files.createTempDirectory("aster-benchmark");

        // 小型模块：简单函数
        smallModule = createSmallModule();

        // 中型模块：多个函数和调用
        mediumModule = createMediumModule();

        // 大型模块：复杂逻辑
        largeModule = createLargeModule();
    }

    @TearDown
    public void teardown() throws IOException {
        // 清理临时文件
        Files.walk(tempDir)
            .sorted((a, b) -> -a.compareTo(b))
            .forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // Ignore
                }
            });
    }

    @Benchmark
    public byte[] compileSmallModule() throws IOException {
        return Main.emitModule(smallModule, tempDir.toString());
    }

    @Benchmark
    public byte[] compileMediumModule() throws IOException {
        return Main.emitModule(mediumModule, tempDir.toString());
    }

    @Benchmark
    public byte[] compileLargeModule() throws IOException {
        return Main.emitModule(largeModule, tempDir.toString());
    }

    // ==================== 测试数据生成 ====================

    /**
     * 小型模块：3 个简单函数
     *
     * func add(a: Int, b: Int) -> Int = a + b
     * func multiply(x: Int, y: Int) -> Int = x * y
     * func main() -> Int = add(1, 2)
     */
    private CoreModel.Module createSmallModule() {
        CoreModel.Module module = new CoreModel.Module();
        module.name = "small";

        // func add(a: Int, b: Int) -> Int = a + b
        CoreModel.Func add = new CoreModel.Func();
        add.name = "add";
        add.params = java.util.List.of(
            createParam("a", "Int"),
            createParam("b", "Int")
        );
        add.ret = createTypeName("Int");
        add.body = createBinaryCall("+", createName("a"), createName("b"));

        // func multiply(x: Int, y: Int) -> Int = x * y
        CoreModel.Func multiply = new CoreModel.Func();
        multiply.name = "multiply";
        multiply.params = java.util.List.of(
            createParam("x", "Int"),
            createParam("y", "Int")
        );
        multiply.ret = createTypeName("Int");
        multiply.body = createBinaryCall("*", createName("x"), createName("y"));

        // func main() -> Int = add(1, 2)
        CoreModel.Func main = new CoreModel.Func();
        main.name = "main";
        main.params = java.util.List.of();
        main.ret = createTypeName("Int");
        main.body = createCallExpr("add", createInt(1), createInt(2));

        module.decls = java.util.List.of(add, multiply, main);
        return module;
    }

    /**
     * 中型模块：10 个函数，包含条件分支和递归
     *
     * func factorial(n: Int) -> Int =
     *   if n < 2 then 1 else n * factorial(n - 1)
     *
     * func fibonacci(n: Int) -> Int =
     *   if n < 2 then n else fibonacci(n - 1) + fibonacci(n - 2)
     *
     * ... 8 more utility functions
     */
    private CoreModel.Module createMediumModule() {
        CoreModel.Module module = new CoreModel.Module();
        module.name = "medium";

        java.util.List<CoreModel.Func> funcs = new java.util.ArrayList<>();

        // func factorial(n: Int) -> Int
        CoreModel.Func factorial = new CoreModel.Func();
        factorial.name = "factorial";
        factorial.params = java.util.List.of(createParam("n", "Int"));
        factorial.ret = createTypeName("Int");

        CoreModel.If factIf = new CoreModel.If();
        factIf.cond = createBinaryCall("<", createName("n"), createInt(2));
        factIf.then_ = createInt(1);
        factIf.else_ = createBinaryCall("*",
            createName("n"),
            createCallExpr("factorial", createBinaryCall("-", createName("n"), createInt(1)))
        );
        factorial.body = factIf;
        funcs.add(factorial);

        // func fibonacci(n: Int) -> Int
        CoreModel.Func fibonacci = new CoreModel.Func();
        fibonacci.name = "fibonacci";
        fibonacci.params = java.util.List.of(createParam("n", "Int"));
        fibonacci.ret = createTypeName("Int");

        CoreModel.If fibIf = new CoreModel.If();
        fibIf.cond = createBinaryCall("<", createName("n"), createInt(2));
        fibIf.then_ = createName("n");
        fibIf.else_ = createBinaryCall("+",
            createCallExpr("fibonacci", createBinaryCall("-", createName("n"), createInt(1))),
            createCallExpr("fibonacci", createBinaryCall("-", createName("n"), createInt(2)))
        );
        fibonacci.body = fibIf;
        funcs.add(fibonacci);

        // 添加 8 个辅助函数
        for (int i = 0; i < 8; i++) {
            CoreModel.Func helper = new CoreModel.Func();
            helper.name = "helper" + i;
            helper.params = java.util.List.of(createParam("x", "Int"));
            helper.ret = createTypeName("Int");
            helper.body = createBinaryCall("+", createName("x"), createInt(i));
            funcs.add(helper);
        }

        module.decls = funcs;
        return module;
    }

    /**
     * 大型模块：50+ 函数，模拟真实应用
     */
    private CoreModel.Module createLargeModule() {
        CoreModel.Module module = new CoreModel.Module();
        module.name = "large";

        java.util.List<CoreModel.Func> funcs = new java.util.ArrayList<>();

        // 添加 50 个函数
        for (int i = 0; i < 50; i++) {
            CoreModel.Func func = new CoreModel.Func();
            func.name = "func" + i;
            func.params = java.util.List.of(
                createParam("a", "Int"),
                createParam("b", "Int")
            );
            func.ret = createTypeName("Int");

            // 交替使用不同的函数体模式
            if (i % 3 == 0) {
                // 简单算术
                func.body = createBinaryCall("+", createName("a"), createName("b"));
            } else if (i % 3 == 1) {
                // 条件分支
                CoreModel.If ifExpr = new CoreModel.If();
                ifExpr.cond = createBinaryCall("<", createName("a"), createName("b"));
                ifExpr.then_ = createName("a");
                ifExpr.else_ = createName("b");
                func.body = ifExpr;
            } else {
                // 函数调用
                int targetFunc = i > 0 ? i - 1 : 0;
                func.body = createCallExpr("func" + targetFunc, createName("a"), createName("b"));
            }

            funcs.add(func);
        }

        module.decls = funcs;
        return module;
    }

    // ==================== 辅助方法 ====================

    private CoreModel.Param createParam(String name, String typeName) {
        CoreModel.Param param = new CoreModel.Param();
        param.name = name;
        param.type = createTypeName(typeName);
        return param;
    }

    private CoreModel.TypeName createTypeName(String name) {
        CoreModel.TypeName type = new CoreModel.TypeName();
        type.name = name;
        return type;
    }

    private CoreModel.Name createName(String name) {
        CoreModel.Name n = new CoreModel.Name();
        n.name = name;
        return n;
    }

    private CoreModel.IntE createInt(int value) {
        CoreModel.IntE i = new CoreModel.IntE();
        i.value = value;
        return i;
    }

    private CoreModel.Call createBinaryCall(String op, CoreModel.Expr left, CoreModel.Expr right) {
        CoreModel.Call call = new CoreModel.Call();
        call.target = createName(op);
        call.args = java.util.List.of(left, right);
        return call;
    }

    private CoreModel.Call createCallExpr(String funcName, CoreModel.Expr... args) {
        CoreModel.Call call = new CoreModel.Call();
        call.target = createName(funcName);
        call.args = java.util.List.of(args);
        return call;
    }
}
