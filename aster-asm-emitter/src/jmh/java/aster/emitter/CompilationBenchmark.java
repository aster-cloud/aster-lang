package aster.emitter;

import aster.core.ir.CoreModel;
import aster.core.typecheck.BuiltinTypes;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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

    private Path tempDir;

    @Setup
    public void setup() throws IOException {
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
    public void compileSmallModule() throws IOException {
        compileModule(smallModule);
    }

    @Benchmark
    public void compileMediumModule() throws IOException {
        compileModule(mediumModule);
    }

    @Benchmark
    public void compileLargeModule() throws IOException {
        compileModule(largeModule);
    }

    /**
     * 简化的编译流程 - 基于 Main.main() 的核心逻辑
     */
    private void compileModule(CoreModel.Module module) throws IOException {
        // 创建上下文（简化版本，不加载hints等配置）
        var context = new ContextBuilder(module);
        Map<String, CoreModel.Func> functionSchemas = new java.util.LinkedHashMap<>();
        for (var d : module.decls) {
            if (d instanceof CoreModel.Func fn) {
                functionSchemas.put(fn.name, fn);
            }
        }

        var ctx = new Main.Ctx(
            tempDir,
            context,
            new java.util.concurrent.atomic.AtomicInteger(0),
            Map.of(),  // 空的函数hints
            new java.util.LinkedHashMap<>(),  // 字符串池
            functionSchemas
        );

        String pkgName = (module.name == null || module.name.isEmpty()) ? "app" : module.name;

        // 编译所有声明
        for (var d : module.decls) {
            if (d instanceof CoreModel.Data data) {
                Main.emitData(ctx, pkgName, data);
            } else if (d instanceof CoreModel.Enum en) {
                Main.emitEnum(ctx, pkgName, en);
            } else if (d instanceof CoreModel.Func fn) {
                Main.emitFunc(ctx, pkgName, module, fn);
            }
        }
    }

    // ==================== 测试数据生成 ====================

    /**
     * 小型模块：3 个简单函数
     *
     * func add(a: Int, b: Int) -> Int { return a }
     * func multiply(x: Int, y: Int) -> Int { return x }
     * func main() -> Int { return 42 }
     */
    private CoreModel.Module createSmallModule() {
        CoreModel.Module module = new CoreModel.Module();
        module.name = "small";

        // func add(a: Int, b: Int) -> Int { return a }
        CoreModel.Func add = new CoreModel.Func();
        add.name = "add";
        add.params = List.of(
            createParam("a", BuiltinTypes.INT),
            createParam("b", BuiltinTypes.INT)
        );
        add.ret = createTypeName(BuiltinTypes.INT);
        add.body = createBlock(createReturn(createName("a")));
        add.effects = List.of();

        // func multiply(x: Int, y: Int) -> Int { return x }
        CoreModel.Func multiply = new CoreModel.Func();
        multiply.name = "multiply";
        multiply.params = List.of(
            createParam("x", BuiltinTypes.INT),
            createParam("y", BuiltinTypes.INT)
        );
        multiply.ret = createTypeName(BuiltinTypes.INT);
        multiply.body = createBlock(createReturn(createName("x")));
        multiply.effects = List.of();

        // func main() -> Int { return 42 }
        CoreModel.Func main = new CoreModel.Func();
        main.name = "main";
        main.params = List.of();
        main.ret = createTypeName(BuiltinTypes.INT);
        main.body = createBlock(createReturn(createInt(42)));
        main.effects = List.of();

        module.decls = List.of(add, multiply, main);
        return module;
    }

    /**
     * 中型模块：20 个函数 + Data + Enum，包含条件分支
     */
    private CoreModel.Module createMediumModule() {
        CoreModel.Module module = new CoreModel.Module();
        module.name = "medium";

        List<CoreModel.Decl> decls = new java.util.ArrayList<>();

        // 添加数据类型：Point(x: Int, y: Int)
        CoreModel.Data pointData = new CoreModel.Data();
        pointData.name = "Point";
        CoreModel.Field xField = new CoreModel.Field();
        xField.name = "x";
        xField.type = createTypeName(BuiltinTypes.INT);
        CoreModel.Field yField = new CoreModel.Field();
        yField.name = "y";
        yField.type = createTypeName(BuiltinTypes.INT);
        pointData.fields = List.of(xField, yField);
        decls.add(pointData);

        // 添加枚举类型：Direction { North, South, East, West }
        CoreModel.Enum directionEnum = new CoreModel.Enum();
        directionEnum.name = "Direction";
        directionEnum.variants = List.of("North", "South", "East", "West");
        decls.add(directionEnum);

        // 添加 20 个函数，交替使用不同的复杂度
        for (int i = 0; i < 20; i++) {
            CoreModel.Func func = new CoreModel.Func();
            func.name = "func" + i;
            func.params = List.of(createParam("x", BuiltinTypes.INT));
            func.ret = createTypeName(BuiltinTypes.INT);
            func.effects = List.of();

            if (i % 3 == 0) {
                // 简单返回
                func.body = createBlock(createReturn(createName("x")));
            } else if (i % 3 == 1) {
                // 条件分支
                CoreModel.If ifStmt = new CoreModel.If();
                ifStmt.cond = createBool(true);
                ifStmt.thenBlock = createBlock(createReturn(createInt(1)));
                ifStmt.elseBlock = createBlock(createReturn(createInt(0)));
                func.body = createBlock(ifStmt);
            } else {
                // 返回常量（简化版递归调用）
                func.body = createBlock(createReturn(createInt(i)));
            }

            decls.add(func);
        }

        module.decls = decls;
        return module;
    }

    /**
     * 大型模块：50+ 函数 + Data + Enum，模拟真实应用
     */
    private CoreModel.Module createLargeModule() {
        CoreModel.Module module = new CoreModel.Module();
        module.name = "large";

        java.util.List<CoreModel.Decl> decls = new java.util.ArrayList<>();

        // 添加数据类型：User(name: String, age: Int, email: String)
        CoreModel.Data userData = new CoreModel.Data();
        userData.name = "User";
        CoreModel.Field nameField = new CoreModel.Field();
        nameField.name = "name";
        nameField.type = createTypeName(BuiltinTypes.STRING);
        CoreModel.Field ageField = new CoreModel.Field();
        ageField.name = "age";
        ageField.type = createTypeName(BuiltinTypes.INT);
        CoreModel.Field emailField = new CoreModel.Field();
        emailField.name = "email";
        emailField.type = createTypeName(BuiltinTypes.STRING);
        userData.fields = List.of(nameField, ageField, emailField);
        decls.add(userData);

        // 添加枚举类型：Status { Active, Inactive, Pending }
        CoreModel.Enum statusEnum = new CoreModel.Enum();
        statusEnum.name = "Status";
        statusEnum.variants = List.of("Active", "Inactive", "Pending");
        decls.add(statusEnum);

        // 添加 50 个函数
        for (int i = 0; i < 50; i++) {
            CoreModel.Func func = new CoreModel.Func();
            func.name = "func" + i;
            func.params = java.util.List.of(
                createParam("a", BuiltinTypes.INT),
                createParam("b", BuiltinTypes.INT)
            );
            func.ret = createTypeName(BuiltinTypes.INT);
            func.effects = List.of();

            // 交替使用不同的函数体模式
            if (i % 3 == 0) {
                // 简单算术
                func.body = createBlock(createReturn(createBinaryCall("+", createName("a"), createName("b"))));
            } else if (i % 3 == 1) {
                // 条件分支
                CoreModel.If ifStmt = new CoreModel.If();
                ifStmt.cond = createBinaryCall("<", createName("a"), createName("b"));
                ifStmt.thenBlock = createBlock(createReturn(createName("a")));
                ifStmt.elseBlock = createBlock(createReturn(createName("b")));
                func.body = createBlock(ifStmt);
            } else {
                // 函数调用
                int targetFunc = i > 0 ? i - 1 : 0;
                func.body = createBlock(createReturn(createCallExpr("func" + targetFunc, createName("a"), createName("b"))));
            }

            decls.add(func);
        }

        module.decls = decls;
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

    private CoreModel.Bool createBool(boolean value) {
        CoreModel.Bool b = new CoreModel.Bool();
        b.value = value;
        return b;
    }

    private CoreModel.Block createBlock(CoreModel.Stmt... stmts) {
        CoreModel.Block block = new CoreModel.Block();
        block.statements = List.of(stmts);
        return block;
    }

    private CoreModel.Return createReturn(CoreModel.Expr expr) {
        CoreModel.Return ret = new CoreModel.Return();
        ret.expr = expr;
        return ret;
    }
}
