package aster.cli.compiler;

import aster.cli.TypeScriptBridge.Result;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

/**
 * Java 编译器后端实现
 * <p>
 * 提供纯 Java 实现的编译管线，逐阶段替代 TypeScript 版本。
 * 当前为骨架实现，各编译阶段将在后续迁移中逐步填充。
 * <p>
 * <b>实现状态</b>：
 * <ul>
 *   <li>Canonicalizer: 待实现</li>
 *   <li>Lexer: 待实现</li>
 *   <li>Parser: 待实现</li>
 *   <li>Lower to Core: 待实现</li>
 *   <li>Type Checker: 待实现</li>
 *   <li>Effect Inference: 待实现</li>
 * </ul>
 */
public final class JavaCompilerBackend implements CompilerBackend {

    /**
     * 记录各编译阶段的实现状态
     * <p>
     * key: 阶段名称（如 "native:cli:class", "typecheck", "parse"）<br>
     * value: 是否已实现
     */
    private final Map<String, Boolean> availableStages;

    /**
     * 模块映射（模块名 → JVM 包名），由 runCompile() 生成，供 runJar() 使用
     */
    private Map<String, String> moduleMapping;

    /**
     * Core IR 模块，由 lowerToCore() 生成，供后续阶段使用
     */
    private aster.core.ir.CoreModel.Module coreModule;

    /**
     * 创建 Java 编译器后端（Phase 2: 纯 Java 实现，不再依赖 TypeScript Bridge）
     */
    public JavaCompilerBackend() {
        this.availableStages = new HashMap<>();
        // 初始化所有阶段为未实现
        registerStage("native:cli:class", true);  // 完整编译管线（Phase 1: 委托给 TS）
        registerStage("native:cli:typecheck", true);  // 类型检查 ✅ 已实现
        registerStage("typecheck", true);  // 类型检查（别名）✅ 已实现
        registerStage("native:cli:jar", true);  // JAR 打包（Phase 1: 委托给 TS）
        registerStage("native:cli:parse", true);  // 解析阶段 ✅ 已实现
        registerStage("parse", true);  // 解析阶段（别名）✅ 已实现
        registerStage("native:cli:core", true);  // Lower to Core 阶段 ✅ 已实现
        registerStage("core", true);  // Lower to Core 阶段（别名）✅ 已实现
        registerStage("canonicalize", true);  // 规范化阶段 ✅ 已实现
        registerStage("lex", true);  // 词法分析 ✅ 已实现
    }

    @Override
    public Result execute(String stage, List<String> args) {
        return execute(stage, args, Map.of());
    }

    @Override
    public Result execute(String stage, List<String> args, Map<String, String> envOverrides) {
        if (!isStageAvailable(stage)) {
            String errorMessage = """
                Java 编译器尚未实现阶段: %s

                当前实现状态:
                  - Canonicalizer: 待实现
                  - Lexer: 待实现
                  - Parser: 待实现
                  - Lower to Core: 待实现
                  - Type Checker: 待实现
                  - Effect Inference: 待实现

                提示: 请设置 ASTER_COMPILER=typescript 使用 TypeScript 编译器。
                """.formatted(stage);

            return new Result(1, "", errorMessage, List.of());
        }

        // TODO: 实现各编译阶段
        // 当各阶段迁移完成后，将在此处调用对应的 Java 实现
        return switch (stage) {
            // Phase 2: 纯 Java 编译管线（compile 和 jar 阶段已实现）
            case "native:cli:class" -> {
                // 完整编译管线：parse → lowerToCore → compile
                if (args.isEmpty()) {
                    yield new Result(1, "", "compile 阶段需要提供输入文件", List.of());
                }

                // 1. Lower to Core IR
                Result coreResult = lowerToCore(args, envOverrides);
                if (coreResult.exitCode() != 0) {
                    yield coreResult;  // 失败时直接返回
                }

                // 2. Compile to bytecode
                yield runCompile(args, envOverrides);
            }

            // Phase 2: JAR 打包阶段（纯 Java 实现）
            case "native:cli:jar" -> runJar(args, envOverrides);

            // Java 实现的阶段
            case "native:cli:typecheck", "typecheck" -> runTypecheck(args, envOverrides);
            case "native:cli:parse", "parse" -> runParser(args, envOverrides);
            case "native:cli:core", "core" -> lowerToCore(args, envOverrides);
            case "canonicalize" -> runCanonicalizer(args, envOverrides);
            case "lex" -> runLexer(args, envOverrides);
            default -> new Result(1, "", "未知的编译阶段: " + stage, List.of());
        };
    }

    @Override
    public String getType() {
        return "java";
    }

    @Override
    public boolean isStageAvailable(String stage) {
        return availableStages.getOrDefault(stage, false);
    }

    /**
     * 注册编译阶段的实现状态
     *
     * @param stage 阶段名称
     * @param available 是否可用
     */
    public void registerStage(String stage, boolean available) {
        availableStages.put(stage, available);
    }

    // ============================================================
    // 各编译阶段实现
    // ============================================================

    private Result runTypecheck(List<String> args, Map<String, String> envOverrides) {
        try {
            if (args.isEmpty()) {
                return new Result(1, "", "typecheck 阶段需要提供输入文件", List.of());
            }

            // 1. 解析 AST
            String inputPath = args.get(0);
            Result parseResult = runParser(List.of(inputPath), envOverrides);
            if (parseResult.exitCode() != 0) {
                return parseResult;  // 解析失败，直接返回错误
            }

            // 2. 反序列化 AST (从 JSON)
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            aster.core.ast.Module astModule = objectMapper.readValue(parseResult.stdout(), aster.core.ast.Module.class);

            // 3. 降级到 Core IR
            aster.core.lowering.CoreLowering lowering = new aster.core.lowering.CoreLowering();
            aster.core.ir.CoreModel.Module coreModule = lowering.lowerModule(astModule);

            // 4. 类型检查
            aster.core.typecheck.TypeChecker typeChecker = new aster.core.typecheck.TypeChecker();
            java.util.List<aster.core.typecheck.model.Diagnostic> diagnostics = typeChecker.typecheckModule(coreModule);

            // 5. 检查是否有错误
            boolean hasErrors = diagnostics.stream()
                .anyMatch(d -> d.severity() == aster.core.typecheck.model.Diagnostic.Severity.ERROR);

            // 6. 格式化诊断信息
            String output = formatDiagnostics(diagnostics);

            // 7. 根据结果返回（退出码 2 表示类型错误）
            if (hasErrors) {
                return new Result(2, "", output, List.of());
            } else {
                return new Result(0, output, "", List.of());
            }
        } catch (java.io.IOException e) {
            return new Result(1, "", "读取文件失败: " + e.getMessage(), List.of());
        } catch (Exception e) {
            return new Result(1, "", "类型检查失败: " + e.getMessage() + "\n" + getStackTrace(e), List.of());
        }
    }

    private String formatDiagnostics(java.util.List<aster.core.typecheck.model.Diagnostic> diagnostics) {
        if (diagnostics.isEmpty()) {
            return "类型检查通过，无错误。";
        }

        var sb = new StringBuilder();
        for (var diag : diagnostics) {
            // 格式: severity: message [at file:line:col]
            sb.append(diag.severity().toString().toLowerCase());
            sb.append(": ");
            sb.append(diag.message());

            if (diag.span().isPresent()) {
                var origin = diag.span().get();
                sb.append(" [at ");
                if (origin.file != null && !origin.file.isEmpty()) {
                    sb.append(origin.file).append(":");
                }
                sb.append(origin.start.line)
                  .append(":")
                  .append(origin.start.col);
                sb.append("]");
            }

            if (diag.help().isPresent()) {
                sb.append("\n  help: ").append(diag.help().get());
            }

            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 执行编译阶段，将 Core IR 编译为 JVM 字节码
     * <p>
     * 前置条件：必须先调用 lowerToCore() 生成 coreModule
     *
     * @param args 命令行参数（可能包含 --out <path>）
     * @param envOverrides 环境变量覆盖
     * @return 编译结果（成功时 exitCode=0，失败时 exitCode=1）
     */
    private Result runCompile(List<String> args, Map<String, String> envOverrides) {
        try {
            // 检查前置条件
            if (this.coreModule == null) {
                return new Result(1, "", "编译失败: 未找到 Core IR 模块，请先执行 lowerToCore() 阶段", List.of());
            }

            // 解析 --out 参数
            java.nio.file.Path outputDir = java.nio.file.Path.of(System.getProperty("user.dir"))
                .resolve("build")
                .resolve("jvm-classes");

            for (int i = 0; i < args.size(); i++) {
                if ("--out".equals(args.get(i)) && i + 1 < args.size()) {
                    outputDir = java.nio.file.Path.of(args.get(i + 1));
                    break;
                }
            }

            // 准备 funcHints（Phase B：基于 Core IR 生成基础类型提示）
            java.util.Map<String, java.util.Map<String, Character>> funcHints = buildFuncHints(this.coreModule);

            // 调用 asm-emitter API
            aster.emitter.Main.CompileResult result = aster.emitter.Main.compile(
                this.coreModule,
                outputDir,
                funcHints
            );

            // 处理编译结果
            if (!result.success) {
                String errorMessage = "编译失败:\n" + String.join("\n", result.errors);
                return new Result(1, "", errorMessage, List.of());
            }

            // 保存 moduleMapping 到实例字段，供 runJar() 使用
            this.moduleMapping = result.moduleMapping;

            // 返回成功（输出字节码文件路径信息）
            String successMessage = String.format(
                "编译成功，字节码已生成到: %s\n模块映射: %s",
                outputDir.toAbsolutePath(),
                result.moduleMapping
            );

            return new Result(0, successMessage, "", List.of());
        } catch (Exception e) {
            return new Result(1, "", "编译失败: " + e.getMessage() + "\n" + getStackTrace(e), List.of());
        }
    }

    private java.util.Map<String, java.util.Map<String, Character>> buildFuncHints(aster.core.ir.CoreModel.Module module) {
        if (module == null || module.decls == null || module.decls.isEmpty()) {
            return java.util.Collections.emptyMap();
        }

        String pkgName = (module.name == null || module.name.isEmpty()) ? "app" : module.name;
        java.util.Map<String, Character> globalHints = new java.util.LinkedHashMap<>();

        for (var decl : module.decls) {
            if (decl instanceof aster.core.ir.CoreModel.Func func) {
                Character hint = toPrimitiveHint(func.ret);
                if (hint != null) {
                    globalHints.put(func.name, hint);
                    globalHints.put(pkgName + "." + func.name, hint);
                }
            }
        }

        if (globalHints.isEmpty()) {
            return java.util.Collections.emptyMap();
        }

        java.util.Map<String, java.util.Map<String, Character>> result = new java.util.LinkedHashMap<>();
        for (var decl : module.decls) {
            if (decl instanceof aster.core.ir.CoreModel.Func func) {
                String key = pkgName + "." + func.name;
                result.put(key, new java.util.LinkedHashMap<>(globalHints));
            }
        }
        return result;
    }

    private Character toPrimitiveHint(aster.core.ir.CoreModel.Type type) {
        if (type instanceof aster.core.ir.CoreModel.TypeName typeName && typeName.name != null) {
            return switch (typeName.name) {
                case "Int" -> 'I';
                case "Bool", "Boolean" -> 'Z';
                case "Long" -> 'J';
                case "Double", "Float" -> 'D'; // Float 同步使用双精度提示，JVM 指令可兼容
                default -> null;
            };
        }
        return null;
    }

    /**
     * 执行 JAR 打包阶段，将字节码和 aster-runtime 打包为可执行 JAR
     * <p>
     * 如果提供了源文件参数，将自动先执行编译管线（parse → lowerToCore → compile）
     *
     * @param args 可选的源文件路径（第一个参数）
     * @param envOverrides 环境变量覆盖
     * @return 打包结果（成功时 exitCode=0，失败时 exitCode=1）
     */
    private Result runJar(List<String> args, Map<String, String> envOverrides) {
        try {
            // 如果提供了源文件，先执行完整编译管线
            if (!args.isEmpty() && !args.get(0).startsWith("--")) {
                // 执行 parse → lowerToCore → compile（使用默认输出目录）
                Result coreResult = lowerToCore(args, envOverrides);
                if (coreResult.exitCode() != 0) {
                    return coreResult;
                }

                // 调用 compile 时不传递 --out 参数（使用默认输出目录）
                List<String> compileArgs = new ArrayList<>();
                compileArgs.add(args.get(0));  // 只传递源文件路径
                Result compileResult = runCompile(compileArgs, envOverrides);
                if (compileResult.exitCode() != 0) {
                    return compileResult;
                }
            }

            // 检查前置条件
            if (this.moduleMapping == null || this.moduleMapping.isEmpty()) {
                return new Result(1, "", "JAR 打包失败: 未找到模块映射，请先执行 compile 阶段", List.of());
            }

            // 解析 --out 参数
            Path workingDir = Path.of(System.getProperty("user.dir"));
            Path classesDir = workingDir.resolve("build/jvm-classes");
            Path jarFile = workingDir.resolve("build/aster-out/aster.jar");

            for (int i = 0; i < args.size(); i++) {
                if ("--out".equals(args.get(i)) && i + 1 < args.size()) {
                    jarFile = Path.of(args.get(i + 1));
                    break;
                }
            }

            Path outDir = jarFile.getParent();

            // 验证 classes 目录存在
            if (!Files.exists(classesDir)) {
                return new Result(1, "", "JAR 打包失败: 字节码目录不存在: " + classesDir, List.of());
            }

            // 创建输出目录
            Files.createDirectories(outDir);

            // 定位 aster-runtime JAR
            Path runtimeJar = findRuntimeJar();
            if (runtimeJar == null || !Files.exists(runtimeJar)) {
                return new Result(1, "", "JAR 打包失败: 未找到 aster-runtime.jar，请先构建: ./gradlew :aster-runtime:jar", List.of());
            }

            // 创建 Manifest（暂不设置 Main-Class，因为模块名未知）
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

            // 打包 JAR（fat JAR：包含项目 classes + aster-runtime）
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toFile()), manifest)) {
                // 1. 添加项目 .class 文件
                addDirectoryToJar(jos, classesDir, classesDir);

                // 2. 合并 aster-runtime JAR（跳过 META-INF 避免冲突）
                mergeJarFile(jos, runtimeJar);

                // 3. 写入 package-map.json（可选）
                if (this.moduleMapping != null && !this.moduleMapping.isEmpty()) {
                    String packageMapJson = generatePackageMapJson(this.moduleMapping);
                    jos.putNextEntry(new JarEntry("aster-asm-emitter/build/aster-out/package-map.json"));
                    jos.write(packageMapJson.getBytes(StandardCharsets.UTF_8));
                    jos.closeEntry();
                }
            }

            // 验证生成
            if (!Files.exists(jarFile)) {
                return new Result(1, "", "JAR 打包失败: 文件未生成", List.of());
            }

            String successMessage = String.format("JAR 打包成功: %s", jarFile.toAbsolutePath());
            return new Result(0, successMessage, "", List.of());

        } catch (IOException e) {
            return new Result(1, "", "JAR 打包失败: " + e.getMessage() + "\n" + getStackTrace(e), List.of());
        } catch (Exception e) {
            return new Result(1, "", "JAR 打包失败: " + e.getMessage() + "\n" + getStackTrace(e), List.of());
        }
    }

    /**
     * 递归添加目录下的所有文件到 JAR
     */
    private void addDirectoryToJar(JarOutputStream jos, Path sourceDir, Path baseDir) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String entryName = baseDir.relativize(file).toString().replace('\\', '/');
                    jos.putNextEntry(new JarEntry(entryName));
                    Files.copy(file, jos);
                    jos.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException("添加文件到 JAR 失败: " + file, e);
                }
            });
        }
    }

    /**
     * 合并另一个 JAR 文件的内容（跳过 META-INF 目录）
     */
    private void mergeJarFile(JarOutputStream jos, Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            jarFile.stream()
                .filter(entry -> !entry.getName().startsWith("META-INF/") && !entry.isDirectory())
                .forEach(entry -> {
                    try {
                        jos.putNextEntry(new JarEntry(entry.getName()));
                        try (InputStream is = jarFile.getInputStream(entry)) {
                            is.transferTo(jos);
                        }
                        jos.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException("合并 JAR 条目失败: " + entry.getName(), e);
                    }
                });
        }
    }

    /**
     * 定位 aster-runtime JAR 文件
     */
    private Path findRuntimeJar() {
        // 优先从 Gradle 构建目录查找
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        Path gradleJar = projectRoot.resolve("aster-runtime/build/libs/aster-runtime.jar");

        if (Files.exists(gradleJar)) {
            return gradleJar;
        }

        // 备用：从 classpath 查找（当作为库使用时）
        String classpath = System.getProperty("java.class.path");
        for (String entry : classpath.split(System.getProperty("path.separator"))) {
            if (entry.contains("aster-runtime") && entry.endsWith(".jar")) {
                Path jar = Path.of(entry);
                if (Files.exists(jar)) {
                    return jar;
                }
            }
        }

        return null;
    }

    /**
     * 生成 package-map.json 内容
     */
    private String generatePackageMapJson(Map<String, String> moduleMapping) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"modules\": [\n");

        int i = 0;
        for (Map.Entry<String, String> entry : moduleMapping.entrySet()) {
            if (i > 0) json.append(",\n");
            json.append("    { \"cnl\": \"").append(entry.getKey()).append("\", ");
            json.append("\"jvm\": \"").append(entry.getValue()).append("\" }");
            i++;
        }

        json.append("\n  ]\n");
        json.append("}\n");
        return json.toString();
    }


    private Result runParser(List<String> args, Map<String, String> envOverrides) {
        try {
            if (args.isEmpty()) {
                return new Result(1, "", "parse 需要提供输入文件", List.of());
            }

            // 读取输入文件
            String inputPath = args.get(0);
            String input = java.nio.file.Files.readString(java.nio.file.Path.of(inputPath));

            // 执行解析
            org.antlr.v4.runtime.CharStream charStream = org.antlr.v4.runtime.CharStreams.fromString(input);
            aster.core.parser.AsterCustomLexer lexer = new aster.core.parser.AsterCustomLexer(charStream);
            org.antlr.v4.runtime.CommonTokenStream tokens = new org.antlr.v4.runtime.CommonTokenStream(lexer);
            aster.core.parser.AsterParser parser = new aster.core.parser.AsterParser(tokens);

            // 移除默认错误监听器并添加自定义监听器以收集错误
            parser.removeErrorListeners();
            var errorListener = new org.antlr.v4.runtime.BaseErrorListener() {
                private final StringBuilder errors = new StringBuilder();

                @Override
                public void syntaxError(org.antlr.v4.runtime.Recognizer<?, ?> recognizer,
                                      Object offendingSymbol,
                                      int line, int charPositionInLine,
                                      String msg,
                                      org.antlr.v4.runtime.RecognitionException e) {
                    errors.append(String.format("Parse error (line %d:%d): %s\n", line, charPositionInLine, msg));
                }

                public String getErrors() {
                    return errors.toString();
                }
            };
            parser.addErrorListener(errorListener);

            // 解析为 AST
            aster.core.parser.AsterParser.ModuleContext moduleCtx = parser.module();

            // 检查是否有解析错误
            if (!errorListener.getErrors().isEmpty()) {
                return new Result(1, "", errorListener.getErrors(), List.of());
            }

            // 构建 AST
            aster.core.parser.AstBuilder builder = new aster.core.parser.AstBuilder();
            aster.core.ast.Module module = builder.visitModule(moduleCtx);

            // 输出 AST（JSON 格式）
            String output = formatModuleAsJson(module);

            return new Result(0, output, "", List.of());
        } catch (java.io.IOException e) {
            return new Result(1, "", "读取文件失败: " + e.getMessage(), List.of());
        } catch (Exception e) {
            return new Result(1, "", "解析失败: " + e.getMessage() + "\n" + getStackTrace(e), List.of());
        }
    }

    private String getStackTrace(Exception e) {
        var sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    private String formatModuleAsJson(aster.core.ast.Module module) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"kind\": \"Module\",\n");
        sb.append("  \"name\": ").append(formatString(module.name())).append(",\n");
        sb.append("  \"span\": ").append(formatSpan(module.span())).append(",\n");
        sb.append("  \"decls\": [\n");
        for (int i = 0; i < module.decls().size(); i++) {
            sb.append("    ").append(formatDecl(module.decls().get(i)));
            if (i < module.decls().size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String formatDecl(aster.core.ast.Decl decl) {
        return switch (decl) {
            case aster.core.ast.Decl.Func func -> formatFuncDecl(func);
            case aster.core.ast.Decl.Data data -> formatDataDecl(data);
            case aster.core.ast.Decl.Enum enumDecl -> formatEnumDecl(enumDecl);
            case aster.core.ast.Decl.TypeAlias typeAlias -> formatTypeAlias(typeAlias);
            case aster.core.ast.Decl.Import importDecl -> formatImportDecl(importDecl);
            default -> "{\"kind\":\"UnknownDecl\"}";
        };
    }

    private String formatFuncDecl(aster.core.ast.Decl.Func func) {
        var sb = new StringBuilder();
        sb.append("{\"kind\":\"Func\"");
        sb.append(",\"name\":").append(formatString(func.name()));
        sb.append(",\"nameSpan\":").append(formatSpan(func.nameSpan()));
        sb.append(",\"typeParams\":").append(formatStringList(func.typeParams()));
        sb.append(",\"params\":").append(formatParameters(func.params()));
        sb.append(",\"retType\":").append(formatType(func.retType()));
        sb.append(",\"body\":").append(func.body() == null ? "null" : formatBlock(func.body()));
        sb.append(",\"effects\":").append(formatStringList(func.effects()));
        sb.append(",\"effectCaps\":").append(formatStringList(func.effectCaps()));
        sb.append(",\"effectCapsExplicit\":").append(func.effectCapsExplicit());
        sb.append(",\"span\":").append(formatSpan(func.span()));
        sb.append("}");
        return sb.toString();
    }

    private String formatDataDecl(aster.core.ast.Decl.Data data) {
        var sb = new StringBuilder();
        sb.append("{\"kind\":\"Data\"");
        sb.append(",\"name\":").append(formatString(data.name()));
        sb.append(",\"fields\":").append(formatFields(data.fields()));
        sb.append(",\"span\":").append(formatSpan(data.span()));
        sb.append("}");
        return sb.toString();
    }

    private String formatEnumDecl(aster.core.ast.Decl.Enum enumDecl) {
        var sb = new StringBuilder();
        sb.append("{\"kind\":\"Enum\"");
        sb.append(",\"name\":").append(formatString(enumDecl.name()));
        sb.append(",\"variants\":").append(formatStringList(enumDecl.variants()));
        sb.append(",\"span\":").append(formatSpan(enumDecl.span()));
        sb.append("}");
        return sb.toString();
    }

    private String formatTypeAlias(aster.core.ast.Decl.TypeAlias typeAlias) {
        var sb = new StringBuilder();
        sb.append("{\"kind\":\"TypeAlias\"");
        sb.append(",\"annotations\":").append(formatStringList(typeAlias.annotations()));
        sb.append(",\"name\":").append(formatString(typeAlias.name()));
        sb.append(",\"type\":").append(formatType(typeAlias.type()));
        sb.append(",\"span\":").append(formatSpan(typeAlias.span()));
        sb.append("}");
        return sb.toString();
    }

    private String formatImportDecl(aster.core.ast.Decl.Import importDecl) {
        var sb = new StringBuilder();
        sb.append("{\"kind\":\"Import\"");
        sb.append(",\"path\":").append(formatString(importDecl.path()));
        sb.append(",\"alias\":").append(formatString(importDecl.alias()));
        sb.append(",\"span\":").append(formatSpan(importDecl.span()));
        sb.append("}");
        return sb.toString();
    }

    private String formatParameters(java.util.List<aster.core.ast.Decl.Parameter> params) {
        if (params == null) return "null";
        var sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < params.size(); i++) {
            sb.append(formatParam(params.get(i)));
            if (i < params.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatFields(java.util.List<aster.core.ast.Decl.Field> fields) {
        if (fields == null) return "null";
        var sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < fields.size(); i++) {
            var field = fields.get(i);
            sb.append("{\"name\":").append(formatString(field.name()))
                .append(",\"type\":").append(formatType(field.type()))
                .append(",\"annotations\":").append(formatAnnotations(field.annotations()))
                .append("}");
            if (i < fields.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatParam(aster.core.ast.Decl.Parameter param) {
        return "{\"name\":" + formatString(param.name())
            + ",\"type\":" + formatType(param.type())
            + ",\"annotations\":" + formatAnnotations(param.annotations())
            + ",\"span\":" + formatSpan(param.span())
            + "}";
    }

    private String formatType(aster.core.ast.Type type) {
        return switch (type) {
            case aster.core.ast.Type.TypeName tn -> "{\"kind\":\"TypeName\",\"name\":"
                + formatString(tn.name()) + ",\"annotations\":" + formatAnnotations(tn.annotations())
                + ",\"span\":" + formatSpan(tn.span()) + "}";
            case aster.core.ast.Type.TypeVar tv -> "{\"kind\":\"TypeVar\",\"name\":"
                + formatString(tv.name()) + ",\"annotations\":" + formatAnnotations(tv.annotations())
                + ",\"span\":" + formatSpan(tv.span()) + "}";
            case aster.core.ast.Type.TypeApp ta -> formatTypeApp(ta);
            case aster.core.ast.Type.Result result -> "{\"kind\":\"Result\",\"ok\":"
                + formatType(result.ok()) + ",\"err\":" + formatType(result.err())
                + ",\"annotations\":" + formatAnnotations(result.annotations())
                + ",\"span\":" + formatSpan(result.span()) + "}";
            case aster.core.ast.Type.Maybe maybe -> "{\"kind\":\"Maybe\",\"type\":"
                + formatType(maybe.type()) + ",\"annotations\":" + formatAnnotations(maybe.annotations())
                + ",\"span\":" + formatSpan(maybe.span()) + "}";
            case aster.core.ast.Type.Option option -> "{\"kind\":\"Option\",\"type\":"
                + formatType(option.type()) + ",\"annotations\":" + formatAnnotations(option.annotations())
                + ",\"span\":" + formatSpan(option.span()) + "}";
            case aster.core.ast.Type.List list -> "{\"kind\":\"List\",\"type\":"
                + formatType(list.type()) + ",\"annotations\":" + formatAnnotations(list.annotations())
                + ",\"span\":" + formatSpan(list.span()) + "}";
            case aster.core.ast.Type.Map map -> "{\"kind\":\"Map\",\"key\":"
                + formatType(map.key()) + ",\"val\":" + formatType(map.val())
                + ",\"annotations\":" + formatAnnotations(map.annotations())
                + ",\"span\":" + formatSpan(map.span()) + "}";
            case aster.core.ast.Type.FuncType ft -> formatFuncType(ft);
            default -> "{\"kind\":\"UnknownType\"}";
        };
    }

    private String formatFuncType(aster.core.ast.Type.FuncType ft) {
        var sb = new StringBuilder();
        sb.append("{\"kind\":\"FuncType\",\"params\":").append(formatTypeList(ft.params()))
            .append(",\"ret\":").append(formatType(ft.ret()))
            .append(",\"annotations\":").append(formatAnnotations(ft.annotations()))
            .append(",\"span\":").append(formatSpan(ft.span()))
            .append("}");
        return sb.toString();
    }

    private String formatTypeApp(aster.core.ast.Type.TypeApp ta) {
        return "{\"kind\":\"TypeApp\",\"base\":" + formatString(ta.base())
            + ",\"args\":" + formatTypeList(ta.args())
            + ",\"annotations\":" + formatAnnotations(ta.annotations())
            + ",\"span\":" + formatSpan(ta.span()) + "}";
    }

    private String formatTypeList(java.util.List<aster.core.ast.Type> types) {
        if (types == null) return "null";
        var sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < types.size(); i++) {
            sb.append(formatType(types.get(i)));
            if (i < types.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatBlock(aster.core.ast.Block block) {
        var sb = new StringBuilder();
        sb.append("{\"kind\":\"Block\",\"statements\":").append(formatStmtList(block.statements()))
            .append(",\"span\":").append(formatSpan(block.span())).append("}");
        return sb.toString();
    }

    private String formatStmtList(java.util.List<aster.core.ast.Stmt> stmts) {
        if (stmts == null) return "null";
        var sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < stmts.size(); i++) {
            sb.append(formatStmt(stmts.get(i)));
            if (i < stmts.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatStmt(aster.core.ast.Stmt stmt) {
        return switch (stmt) {
            case aster.core.ast.Block block -> formatBlock(block);
            case aster.core.ast.Stmt.Let let -> "{\"kind\":\"Let\",\"name\":"
                + formatString(let.name()) + ",\"expr\":" + formatExpr(let.expr())
                + ",\"span\":" + formatSpan(let.span()) + "}";
            case aster.core.ast.Stmt.Set set -> "{\"kind\":\"Set\",\"name\":"
                + formatString(set.name()) + ",\"expr\":" + formatExpr(set.expr())
                + ",\"span\":" + formatSpan(set.span()) + "}";
            case aster.core.ast.Stmt.Return ret -> "{\"kind\":\"Return\",\"expr\":"
                + formatExpr(ret.expr()) + ",\"span\":" + formatSpan(ret.span()) + "}";
            case aster.core.ast.Stmt.If ifStmt -> formatIfStmt(ifStmt);
            case aster.core.ast.Stmt.Match match -> formatMatchStmt(match);
            case aster.core.ast.Stmt.Start start -> "{\"kind\":\"Start\",\"name\":"
                + formatString(start.name()) + ",\"expr\":" + formatExpr(start.expr())
                + ",\"span\":" + formatSpan(start.span()) + "}";
            case aster.core.ast.Stmt.Wait wait -> "{\"kind\":\"Wait\",\"names\":"
                + formatStringList(wait.names()) + ",\"span\":" + formatSpan(wait.span()) + "}";
            default -> "{\"kind\":\"UnknownStmt\"}";
        };
    }

    private String formatIfStmt(aster.core.ast.Stmt.If ifStmt) {
        var sb = new StringBuilder();
        sb.append("{\"kind\":\"If\",\"cond\":").append(formatExpr(ifStmt.cond()))
            .append(",\"thenBlock\":").append(formatBlock(ifStmt.thenBlock()))
            .append(",\"elseBlock\":")
            .append(ifStmt.elseBlock() == null ? "null" : formatBlock(ifStmt.elseBlock()))
            .append(",\"span\":").append(formatSpan(ifStmt.span()))
            .append("}");
        return sb.toString();
    }

    private String formatMatchStmt(aster.core.ast.Stmt.Match match) {
        var sb = new StringBuilder();
        sb.append("{\"kind\":\"Match\",\"expr\":").append(formatExpr(match.expr()))
            .append(",\"cases\":").append(formatCases(match.cases()))
            .append(",\"span\":").append(formatSpan(match.span()))
            .append("}");
        return sb.toString();
    }

    private String formatCases(java.util.List<aster.core.ast.Stmt.Case> cases) {
        if (cases == null) return "null";
        var sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < cases.size(); i++) {
            sb.append(formatCase(cases.get(i)));
            if (i < cases.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatCase(aster.core.ast.Stmt.Case caseNode) {
        return "{\"pattern\":" + formatPattern(caseNode.pattern())
            + ",\"body\":" + formatCaseBody(caseNode.body())
            + ",\"span\":" + formatSpan(caseNode.span()) + "}";
    }

    private String formatCaseBody(aster.core.ast.Stmt.Case.CaseBody body) {
        if (body instanceof aster.core.ast.Stmt stmtBody) {
            return formatStmt(stmtBody);
        }
        return "{\"kind\":\"UnknownCaseBody\"}";
    }

    private String formatPattern(aster.core.ast.Pattern pattern) {
        return switch (pattern) {
            case aster.core.ast.Pattern.PatternNull pn -> "{\"kind\":\"PatternNull\",\"span\":"
                + formatSpan(pn.span()) + "}";
            case aster.core.ast.Pattern.PatternName pn -> "{\"kind\":\"PatternName\",\"name\":"
                + formatString(pn.name()) + ",\"span\":" + formatSpan(pn.span()) + "}";
            case aster.core.ast.Pattern.PatternInt pi -> "{\"kind\":\"PatternInt\",\"value\":"
                + pi.value() + ",\"span\":" + formatSpan(pi.span()) + "}";
            case aster.core.ast.Pattern.PatternCtor pc -> {
                var sb = new StringBuilder();
                sb.append("{\"kind\":\"PatternCtor\",\"typeName\":").append(formatString(pc.typeName()))
                    .append(",\"names\":").append(formatStringList(pc.names()))
                    .append(",\"args\":").append(formatPatternList(pc.args()))
                    .append(",\"span\":").append(formatSpan(pc.span()))
                    .append("}");
                yield sb.toString();
            }
            default -> "{\"kind\":\"UnknownPattern\"}";
        };
    }

    private String formatPatternList(java.util.List<aster.core.ast.Pattern> patterns) {
        if (patterns == null) return "null";
        var sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < patterns.size(); i++) {
            sb.append(formatPattern(patterns.get(i)));
            if (i < patterns.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatExpr(aster.core.ast.Expr expr) {
        return switch (expr) {
            case aster.core.ast.Expr.Name name -> "{\"kind\":\"Name\",\"name\":"
                + formatString(name.name()) + ",\"span\":" + formatSpan(name.span()) + "}";
            case aster.core.ast.Expr.Bool boolExpr -> "{\"kind\":\"Bool\",\"value\":"
                + boolExpr.value() + ",\"span\":" + formatSpan(boolExpr.span()) + "}";
            case aster.core.ast.Expr.Int intExpr -> "{\"kind\":\"Int\",\"value\":"
                + intExpr.value() + ",\"span\":" + formatSpan(intExpr.span()) + "}";
            case aster.core.ast.Expr.Long longExpr -> "{\"kind\":\"Long\",\"value\":"
                + longExpr.value() + ",\"span\":" + formatSpan(longExpr.span()) + "}";
            case aster.core.ast.Expr.Double doubleExpr -> "{\"kind\":\"Double\",\"value\":"
                + doubleExpr.value() + ",\"span\":" + formatSpan(doubleExpr.span()) + "}";
            case aster.core.ast.Expr.String str -> "{\"kind\":\"String\",\"value\":"
                + formatString(str.value()) + ",\"span\":" + formatSpan(str.span()) + "}";
            case aster.core.ast.Expr.Null nullExpr -> "{\"kind\":\"Null\",\"span\":"
                + formatSpan(nullExpr.span()) + "}";
            case aster.core.ast.Expr.Call call -> formatCall(call);
            case aster.core.ast.Expr.Construct construct -> formatConstruct(construct);
            case aster.core.ast.Expr.Ok ok -> formatSingleExprNode("Ok", ok.expr(), ok.span());
            case aster.core.ast.Expr.Err err -> formatSingleExprNode("Err", err.expr(), err.span());
            case aster.core.ast.Expr.Some some -> formatSingleExprNode("Some", some.expr(), some.span());
            case aster.core.ast.Expr.None none -> "{\"kind\":\"None\",\"span\":"
                + formatSpan(none.span()) + "}";
            case aster.core.ast.Expr.Lambda lambda -> formatLambda(lambda);
            case aster.core.ast.Expr.Await await -> formatSingleExprNode("Await", await.expr(), await.span());
            default -> "{\"kind\":\"UnknownExpr\"}";
        };
    }

    private String formatCall(aster.core.ast.Expr.Call call) {
        var sb = new StringBuilder();
        sb.append("{\"kind\":\"Call\",\"target\":").append(formatExpr(call.target()))
            .append(",\"args\":").append(formatExprList(call.args()))
            .append(",\"span\":").append(formatSpan(call.span()))
            .append("}");
        return sb.toString();
    }

    private String formatConstruct(aster.core.ast.Expr.Construct construct) {
        var sb = new StringBuilder();
        sb.append("{\"kind\":\"Construct\",\"typeName\":").append(formatString(construct.typeName()))
            .append(",\"fields\":").append(formatConstructFields(construct.fields()))
            .append(",\"span\":").append(formatSpan(construct.span()))
            .append("}");
        return sb.toString();
    }

    private String formatLambda(aster.core.ast.Expr.Lambda lambda) {
        var sb = new StringBuilder();
        sb.append("{\"kind\":\"Lambda\",\"params\":").append(formatParameters(lambda.params()))
            .append(",\"retType\":").append(formatType(lambda.retType()))
            .append(",\"body\":").append(formatBlock(lambda.body()))
            .append(",\"span\":").append(formatSpan(lambda.span()))
            .append("}");
        return sb.toString();
    }

    private String formatSingleExprNode(String kind, aster.core.ast.Expr expr, aster.core.ast.Span span) {
        return "{\"kind\":\"" + kind + "\",\"expr\":" + formatExpr(expr)
            + ",\"span\":" + formatSpan(span) + "}";
    }

    private String formatExprList(java.util.List<aster.core.ast.Expr> exprs) {
        if (exprs == null) return "null";
        var sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < exprs.size(); i++) {
            sb.append(formatExpr(exprs.get(i)));
            if (i < exprs.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatConstructFields(java.util.List<aster.core.ast.Expr.Construct.ConstructField> fields) {
        if (fields == null) return "null";
        var sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < fields.size(); i++) {
            var field = fields.get(i);
            sb.append("{\"name\":").append(formatString(field.name()))
                .append(",\"expr\":").append(formatExpr(field.expr()))
                .append("}");
            if (i < fields.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatSpan(aster.core.ast.Span span) {
        if (span == null) return "null";
        return "{\"start\":" + formatPosition(span.start()) + ",\"end\":" + formatPosition(span.end()) + "}";
    }

    private String formatPosition(aster.core.ast.Span.Position position) {
        if (position == null) return "null";
        return "{\"line\":" + position.line() + ",\"col\":" + position.col() + "}";
    }

    private String formatString(String str) {
        if (str == null) return "null";
        return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private String formatStringList(java.util.List<String> values) {
        if (values == null) return "null";
        var sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append(formatString(values.get(i)));
            if (i < values.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatAnnotations(java.util.List<aster.core.ast.Annotation> annotations) {
        if (annotations == null) return "null";
        var sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < annotations.size(); i++) {
            sb.append(formatAnnotation(annotations.get(i)));
            if (i < annotations.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatAnnotation(aster.core.ast.Annotation annotation) {
        return "{\"name\":" + formatString(annotation.name())
            + ",\"params\":" + formatAnnotationParams(annotation.params()) + "}";
    }

    private String formatAnnotationParams(java.util.Map<String, Object> params) {
        if (params == null) return "null";
        var entries = new java.util.ArrayList<>(params.entrySet());
        var sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            sb.append(formatString(entry.getKey())).append(":").append(formatAny(entry.getValue()));
            if (i < entries.size() - 1) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    private String formatAny(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) {
            return formatString(s);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof java.util.Map<?, ?> map) {
            var sb = new StringBuilder();
            sb.append("{");
            var iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                sb.append(formatString(String.valueOf(entry.getKey())))
                    .append(":")
                    .append(formatAny(entry.getValue()));
                if (iterator.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("}");
            return sb.toString();
        }
        if (value instanceof java.util.List<?> list) {
            var sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                sb.append(formatAny(list.get(i)));
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
        return formatString(value.toString());
    }

    private Result lowerToCore(List<String> args, Map<String, String> envOverrides) {
        try {
            if (args.isEmpty()) {
                return new Result(1, "", "core 阶段需要提供输入文件", List.of());
            }

            // 1. 解析 AST
            String inputPath = args.get(0);
            Result parseResult = runParser(List.of(inputPath), envOverrides);
            if (parseResult.exitCode() != 0) {
                return parseResult;  // 解析失败，直接返回错误
            }

            // 2. 反序列化 AST (从 JSON)
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            aster.core.ast.Module astModule = objectMapper.readValue(parseResult.stdout(), aster.core.ast.Module.class);

            // 3. 降级到 Core IR
            aster.core.lowering.CoreLowering lowering = new aster.core.lowering.CoreLowering();
            aster.core.ir.CoreModel.Module module = lowering.lowerModule(astModule);

            // 保存到实例字段，供 runCompile() 使用
            this.coreModule = module;

            // 4. 序列化 Core IR 为 JSON
            String coreJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(module);

            return new Result(0, coreJson, "", List.of());
        } catch (java.io.IOException e) {
            return new Result(1, "", "读取文件失败: " + e.getMessage(), List.of());
        } catch (Exception e) {
            return new Result(1, "", "Core IR 降级失败: " + e.getMessage() + "\n" + getStackTrace(e), List.of());
        }
    }

    private Result runCanonicalizer(List<String> args, Map<String, String> envOverrides) {
        try {
            if (args.isEmpty()) {
                return new Result(1, "", "canonicalize 需要提供输入文件", List.of());
            }

            // 读取输入文件
            String inputPath = args.get(0);
            String input = java.nio.file.Files.readString(java.nio.file.Path.of(inputPath));

            // 执行规范化
            var canonicalizer = new aster.core.canonicalizer.Canonicalizer();
            String output = canonicalizer.canonicalize(input);

            return new Result(0, output, "", List.of());
        } catch (java.io.IOException e) {
            return new Result(1, "", "读取文件失败: " + e.getMessage(), List.of());
        } catch (Exception e) {
            return new Result(1, "", "规范化失败: " + e.getMessage(), List.of());
        }
    }

    private Result runLexer(List<String> args, Map<String, String> envOverrides) {
        try {
            if (args.isEmpty()) {
                return new Result(1, "", "lex 需要提供输入文件", List.of());
            }

            // 读取输入文件
            String inputPath = args.get(0);
            String input = java.nio.file.Files.readString(java.nio.file.Path.of(inputPath));

            // 执行词法分析
            var tokens = aster.core.lexer.Lexer.lex(input);

            // 输出 token 流（JSON 格式）
            StringBuilder output = new StringBuilder();
            output.append("[\n");
            for (int i = 0; i < tokens.size(); i++) {
                var token = tokens.get(i);
                output.append("  {\"kind\":\"").append(token.kind()).append("\"");
                if (token.value() != null) {
                    output.append(",\"value\":").append(formatTokenValue(token.value()));
                }
                output.append(",\"start\":{\"line\":").append(token.start().line())
                      .append(",\"col\":").append(token.start().col()).append("}");
                output.append(",\"end\":{\"line\":").append(token.end().line())
                      .append(",\"col\":").append(token.end().col()).append("}");
                if (token.channel() != null) {
                    output.append(",\"channel\":\"").append(token.channel()).append("\"");
                }
                output.append("}");
                if (i < tokens.size() - 1) {
                    output.append(",");
                }
                output.append("\n");
            }
            output.append("]\n");

            return new Result(0, output.toString(), "", List.of());
        } catch (java.io.IOException e) {
            return new Result(1, "", "读取文件失败: " + e.getMessage(), List.of());
        } catch (aster.core.lexer.LexerException e) {
            return new Result(1, "", "词法分析失败: " + e.getMessage(), List.of());
        } catch (Exception e) {
            return new Result(1, "", "词法分析失败: " + e.getMessage(), List.of());
        }
    }

    private String formatTokenValue(Object value) {
        if (value instanceof String) {
            // 转义字符串中的特殊字符
            String str = (String) value;
            str = str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            return "\"" + str + "\"";
        } else if (value instanceof aster.core.lexer.CommentValue cv) {
            return String.format("{\"raw\":\"%s\",\"text\":\"%s\",\"trivia\":\"%s\"}",
                cv.raw().replace("\\", "\\\\").replace("\"", "\\\""),
                cv.text().replace("\\", "\\\\").replace("\"", "\\\""),
                cv.trivia());
        } else {
            return String.valueOf(value);
        }
    }
}
