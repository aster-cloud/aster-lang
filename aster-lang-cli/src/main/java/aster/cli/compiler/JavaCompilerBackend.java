package aster.cli.compiler;

import aster.cli.TypeScriptBridge.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 创建 Java 编译器后端
     */
    public JavaCompilerBackend() {
        this.availableStages = new HashMap<>();
        // 初始化所有阶段为未实现
        registerStage("native:cli:class", false);  // 完整编译管线
        registerStage("native:cli:typecheck", false);  // 类型检查
        registerStage("native:cli:jar", false);  // JAR 打包
        registerStage("native:cli:parse", true);  // 解析阶段 ✅ 已实现
        registerStage("parse", true);  // 解析阶段（别名）✅ 已实现
        registerStage("core", false);  // Lower to Core 阶段
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
            case "native:cli:class" -> compileToJvmClasses(args, envOverrides);
            case "native:cli:typecheck" -> runTypecheck(args, envOverrides);
            case "native:cli:jar" -> createJar(args, envOverrides);
            case "native:cli:parse", "parse" -> runParser(args, envOverrides);
            case "core" -> lowerToCore(args, envOverrides);
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
    // 各编译阶段实现（占位方法，待迁移时填充）
    // ============================================================

    private Result compileToJvmClasses(List<String> args, Map<String, String> envOverrides) {
        // TODO: 实现完整编译管线
        // 1. Parse → AST
        // 2. Canonicalize → Normalized AST
        // 3. Type Check → Typed AST
        // 4. Lower to Core → Core IR
        // 5. Effect Inference → Core IR with effects
        // 6. Emit JVM Classes → .class files
        throw new UnsupportedOperationException("compile to JVM classes 尚未实现");
    }

    private Result runTypecheck(List<String> args, Map<String, String> envOverrides) {
        // TODO: 实现类型检查
        throw new UnsupportedOperationException("typecheck 尚未实现");
    }

    private Result createJar(List<String> args, Map<String, String> envOverrides) {
        // TODO: 实现 JAR 打包
        throw new UnsupportedOperationException("jar 创建尚未实现");
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
                    errors.append(String.format("解析错误 (line %d:%d): %s\n", line, charPositionInLine, msg));
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
        // TODO: 实现 Lower to Core
        throw new UnsupportedOperationException("lower to core 尚未实现");
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
