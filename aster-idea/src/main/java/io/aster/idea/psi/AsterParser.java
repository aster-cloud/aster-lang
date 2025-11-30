package io.aster.idea.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Aster PSI 解析器
 * <p>
 * 使用 PsiBuilder 构建 PSI 树结构。
 * 解析器识别关键字来确定声明和语句类型，并创建相应的 PSI 节点。
 */
public class AsterParser implements PsiParser {

    /**
     * 检查当前 token 是否为标识符类型（IDENT 或 TYPE_IDENT）
     * 自然语言关键字如 with/produce/and/as/one/of 可能因首字母大小写被识别为不同类型
     */
    private boolean isIdentifier(PsiBuilder builder) {
        IElementType type = builder.getTokenType();
        return type == AsterTokenTypes.IDENT || type == AsterTokenTypes.TYPE_IDENT;
    }

    @Override
    public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        PsiBuilder.Marker rootMarker = builder.mark();

        while (!builder.eof()) {
            parseTopLevel(builder);
        }

        rootMarker.done(root);
        return builder.getTreeBuilt();
    }

    /**
     * 解析顶层元素（声明）
     * <p>
     * 支持 Aster 自然语言语法：
     * - This module is name. -> 模块声明
     * - To funcName with ..., produce ...: -> 函数声明
     * - Define a TypeName with ... -> 数据类型声明
     * - Use module. -> 导入声明
     */
    private void parseTopLevel(PsiBuilder builder) {
        // 跳过空白和换行
        skipWhitespaceAndNewlines(builder);

        if (builder.eof()) {
            return;
        }

        String tokenText = builder.getTokenText();
        IElementType tokenType = builder.getTokenType();

        // 根据标识符文本识别声明类型（Aster 关键字在词法级别可能是 IDENT 或 TYPE_IDENT）
        // 同时支持 IDENT 和 TYPE_IDENT（首字母大写的关键词如 This、Module）
        if ((tokenType == AsterTokenTypes.IDENT || tokenType == AsterTokenTypes.TYPE_IDENT)
                && tokenText != null) {
            switch (tokenText.toLowerCase()) {
                // 自然语言语法（aster-core 官方语法）
                case "this" -> parseModuleDecl(builder);      // This module is name.
                case "to" -> parseFuncDecl(builder);          // To funcName with ..., produce ...
                case "define" -> parseDataDecl(builder);      // Define a TypeName with ...
                case "use" -> parseImportDecl(builder);       // Use module.

                // 兼容传统语法（保留向后兼容）
                case "module" -> parseModuleDeclLegacy(builder);
                case "capabilities" -> parseCapabilitiesDecl(builder);
                case "workflow" -> parseWorkflowDecl(builder);
                case "func" -> parseFuncDeclLegacy(builder);
                case "data" -> parseDataDeclLegacy(builder);
                case "enum" -> parseEnumDecl(builder);
                case "type" -> parseTypeAliasDecl(builder);
                case "import" -> parseImportDeclLegacy(builder);
                default -> builder.advanceLexer(); // 其他标识符跳过
            }
        } else if (tokenType == AsterTokenTypes.COMMENT) {
            // 跳过注释
            builder.advanceLexer();
        } else if (tokenType == AsterTokenTypes.AT) {
            // 注解，跳过并继续解析下一个声明
            parseAnnotation(builder);
        } else {
            // 其他情况，跳过
            builder.advanceLexer();
        }
    }

    /**
     * 解析函数声明（自然语言语法）
     * 语法: To funcName with param: Type and param2: Type, produce ReturnType:
     */
    private void parseFuncDecl(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'To' 关键字
        builder.advanceLexer();

        // 函数名（支持 IDENT 和 TYPE_IDENT）
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
        } else {
            builder.error("期望函数名");
        }

        // 解析自然语言风格的参数列表和返回类型
        parseFuncDeclNaturalRest(builder);

        marker.done(AsterElementTypes.FUNC_DECL);
    }

    /**
     * 解析函数声明（传统语法，保留向后兼容）
     * 语法: func name(params) -> Type:
     */
    private void parseFuncDeclLegacy(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'func' 关键字
        builder.advanceLexer();

        // 函数名（支持 IDENT 和 TYPE_IDENT）
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
        } else {
            builder.error("期望函数名");
        }

        // 解析参数列表和函数体
        parseFuncRest(builder);

        marker.done(AsterElementTypes.FUNC_DECL);
    }

    /**
     * 解析自然语言风格的函数参数和返回类型
     * 语法: with param: Type and param2: Type, produce ReturnType:
     */
    private void parseFuncDeclNaturalRest(PsiBuilder builder) {
        // 'with' 关键字（可选，如果有参数）
        if (isIdentifier(builder) &&
            "with".equalsIgnoreCase(builder.getTokenText())) {
            builder.advanceLexer();

            // 解析参数列表
            parseNaturalParameterList(builder);
        }

        // 逗号分隔符
        if (builder.getTokenType() == AsterTokenTypes.COMMA) {
            builder.advanceLexer();
        }

        // 'produce' 关键字（返回类型）
        if (isIdentifier(builder) &&
            "produce".equalsIgnoreCase(builder.getTokenText())) {
            builder.advanceLexer();

            // 解析返回类型
            parseNaturalReturnType(builder);
        }

        // 冒号和函数体
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseBlock(builder);
        }
    }

    /**
     * 解析自然语言风格的参数列表
     * 语法: param: Type and param2: Type
     */
    private void parseNaturalParameterList(PsiBuilder builder) {
        // 解析第一个参数（支持 IDENT 和 TYPE_IDENT，允许 CamelCase 参数名）
        if (isIdentifier(builder)) {
            parseNaturalParameter(builder);

            // 解析更多参数（用 'and' 分隔）
            while (isIdentifier(builder) &&
                   "and".equalsIgnoreCase(builder.getTokenText())) {
                builder.advanceLexer();
                parseNaturalParameter(builder);
            }
        }
    }

    /**
     * 解析单个自然语言风格的参数
     * 语法: param: Type
     */
    private void parseNaturalParameter(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 参数名（支持 IDENT 和 TYPE_IDENT，允许 CamelCase 参数名）
        if (isIdentifier(builder)) {
            builder.advanceLexer();
        } else {
            builder.error("期望参数名");
            marker.drop();
            return;
        }

        // 冒号
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
        } else {
            builder.error("期望 ':'");
        }

        // 类型
        parseType(builder);

        marker.done(AsterElementTypes.PARAMETER);
    }

    /**
     * 解析自然语言风格的返回类型
     * 语法: Type 或 maybe Type 或 list of Type
     */
    private void parseNaturalReturnType(PsiBuilder builder) {
        // 检查修饰符（支持 IDENT 和 TYPE_IDENT）
        if (isIdentifier(builder)) {
            String text = builder.getTokenText();
            if ("maybe".equalsIgnoreCase(text) || "list".equalsIgnoreCase(text)) {
                builder.advanceLexer();
                // 'of' 关键字（用于 list of Type）
                if (isIdentifier(builder) &&
                    "of".equalsIgnoreCase(builder.getTokenText())) {
                    builder.advanceLexer();
                }
            }
        }

        // 类型名
        parseType(builder);
    }

    /**
     * 解析函数其余部分（参数、返回类型、函数体）
     */
    private void parseFuncRest(PsiBuilder builder) {
        // 类型参数 [T, U]
        if (builder.getTokenType() == AsterTokenTypes.LBRACKET) {
            parseTypeParams(builder);
        }

        // 参数列表 (...)
        if (builder.getTokenType() == AsterTokenTypes.LPAREN) {
            parseParameterList(builder);
        }

        // 效应列表 ?IO, ?State
        if (builder.getTokenType() == AsterTokenTypes.QUESTION) {
            PsiBuilder.Marker effectMarker = builder.mark();
            while (builder.getTokenType() == AsterTokenTypes.QUESTION) {
                builder.advanceLexer();
                if (builder.getTokenType() == AsterTokenTypes.TYPE_IDENT ||
                    builder.getTokenType() == AsterTokenTypes.IDENT) {
                    builder.advanceLexer();
                } else {
                    builder.error("期望效应名称");
                }
            }
            effectMarker.done(AsterElementTypes.EFFECT_LIST);
        }

        // 返回类型 -> Type
        if (builder.getTokenType() == AsterTokenTypes.MINUS) {
            builder.advanceLexer();
            if (builder.getTokenType() == AsterTokenTypes.GT) {
                builder.advanceLexer();
                parseType(builder);
            } else {
                builder.error("期望 '>'（箭头的一部分）");
                // 尝试恢复：如果后面是类型，继续解析
                if (builder.getTokenType() == AsterTokenTypes.TYPE_IDENT ||
                    builder.getTokenType() == AsterTokenTypes.IDENT) {
                    parseType(builder);
                }
            }
        } else if (builder.getTokenType() == AsterTokenTypes.TYPE_IDENT ||
                   builder.getTokenType() == AsterTokenTypes.IDENT) {
            // 可能是用户忘记写 '->'，直接写了类型名
            builder.error("期望 '->' 后跟返回类型");
            parseType(builder);
        }

        // 冒号和函数体
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseBlock(builder);
        } else if (builder.getTokenType() == AsterTokenTypes.NEWLINE) {
            // 缺少冒号但可能有函数体，报告错误
            builder.error("期望 ':'");
        }
    }

    /**
     * 解析数据类型声明（自然语言语法）
     * 语法1: Define a TypeName with field: Type and field2: Type.
     * 语法2: Define TypeName as one of: Variant1, Variant2.
     */
    private void parseDataDecl(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'Define' 关键字
        builder.advanceLexer();

        // 'a' 或 'an' 冠词（可选）
        boolean hasArticle = false;
        if (builder.getTokenType() == AsterTokenTypes.IDENT) {
            String text = builder.getTokenText();
            if ("a".equalsIgnoreCase(text) || "an".equalsIgnoreCase(text)) {
                builder.advanceLexer();
                hasArticle = true;
            }
        }

        // 类型名
        if (builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
        } else if (builder.getTokenType() == AsterTokenTypes.IDENT) {
            // 某些类型名可能被识别为 IDENT
            builder.advanceLexer();
        } else {
            builder.error("期望数据类型名");
        }

        // 检查是 "as one of:" 枚举语法还是 "with" 数据语法
        if (isIdentifier(builder) &&
            "as".equalsIgnoreCase(builder.getTokenText())) {
            // "Define TypeName as one of: Variant1, Variant2."
            builder.advanceLexer(); // 消耗 'as'

            // 'one' 关键字
            if (isIdentifier(builder) &&
                "one".equalsIgnoreCase(builder.getTokenText())) {
                builder.advanceLexer();
            } else {
                builder.error("期望 'one'");
            }

            // 'of' 关键字
            if (isIdentifier(builder) &&
                "of".equalsIgnoreCase(builder.getTokenText())) {
                builder.advanceLexer();
            } else {
                builder.error("期望 'of'");
            }

            // ':' 和变体列表
            if (builder.getTokenType() == AsterTokenTypes.COLON) {
                builder.advanceLexer();
                parseNaturalVariantList(builder);
            } else {
                builder.error("期望 ':'");
            }

            // 结束的句号（可选）
            if (builder.getTokenType() == AsterTokenTypes.DOT) {
                builder.advanceLexer();
            }

            marker.done(AsterElementTypes.ENUM_DECL);
        } else if (isIdentifier(builder) &&
            "with".equalsIgnoreCase(builder.getTokenText())) {
            // "Define a TypeName with field: Type."
            builder.advanceLexer();

            // 解析自然语言风格的字段列表
            parseNaturalFieldList(builder);

            // 结束的句号（可选）
            if (builder.getTokenType() == AsterTokenTypes.DOT) {
                builder.advanceLexer();
            }

            marker.done(AsterElementTypes.DATA_DECL);
        } else {
            // 结束的句号（可选）
            if (builder.getTokenType() == AsterTokenTypes.DOT) {
                builder.advanceLexer();
            }
            marker.done(AsterElementTypes.DATA_DECL);
        }
    }

    /**
     * 解析自然语言风格的变体列表（用于枚举）
     * 语法: Variant1, Variant2, Variant3
     */
    private void parseNaturalVariantList(PsiBuilder builder) {
        skipNewlines(builder);

        // 解析变体列表，用逗号分隔
        while (!builder.eof() && !isTopLevelKeyword(builder)) {
            if (builder.getTokenType() == AsterTokenTypes.DOT) {
                break; // 遇到句号结束
            }

            if (builder.getTokenType() == AsterTokenTypes.IDENT ||
                builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                PsiBuilder.Marker variantMarker = builder.mark();
                builder.advanceLexer();
                variantMarker.done(AsterElementTypes.ENUM_VARIANT);

                // 逗号分隔或 'and' 分隔
                if (builder.getTokenType() == AsterTokenTypes.COMMA) {
                    builder.advanceLexer();
                } else if (isIdentifier(builder) &&
                           "and".equalsIgnoreCase(builder.getTokenText())) {
                    builder.advanceLexer();
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    /**
     * 解析自然语言风格的字段列表
     * 语法: field: Type and field2: Type
     */
    private void parseNaturalFieldList(PsiBuilder builder) {
        // 解析第一个字段（支持 IDENT 和 TYPE_IDENT，允许 CamelCase 字段名）
        if (isIdentifier(builder)) {
            parseNaturalField(builder);

            // 解析更多字段（用 'and' 分隔）
            while (isIdentifier(builder) &&
                   "and".equalsIgnoreCase(builder.getTokenText())) {
                builder.advanceLexer();
                parseNaturalField(builder);
            }
        }
    }

    /**
     * 解析单个自然语言风格的字段
     * 语法: field: Type
     */
    private void parseNaturalField(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 字段名（支持 IDENT 和 TYPE_IDENT，允许 CamelCase 字段名）
        if (isIdentifier(builder)) {
            builder.advanceLexer();
        } else {
            builder.error("期望字段名");
            marker.drop();
            return;
        }

        // 冒号
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
        } else {
            builder.error("期望 ':'");
        }

        // 类型
        parseType(builder);

        marker.done(AsterElementTypes.FIELD_DEF);
    }

    /**
     * 解析数据类型声明（传统语法，保留向后兼容）
     * 语法: data TypeName:
     *         field: Type
     */
    private void parseDataDeclLegacy(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'data' 关键字
        builder.advanceLexer();

        // 类型名
        if (builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
        } else {
            builder.error("期望数据类型名（大写开头）");
        }

        // 可选的泛型参数 [T, E]
        if (builder.getTokenType() == AsterTokenTypes.LBRACKET) {
            parseTypeParams(builder);
        }

        // 冒号和字段列表
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseFieldList(builder);
        } else {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.DATA_DECL);
    }

    /**
     * 解析枚举声明
     */
    private void parseEnumDecl(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'enum' 关键字
        builder.advanceLexer();

        // 枚举名（支持 TYPE_IDENT 和 IDENT）
        if (builder.getTokenType() == AsterTokenTypes.TYPE_IDENT ||
            builder.getTokenType() == AsterTokenTypes.IDENT) {
            builder.advanceLexer();
        } else {
            builder.error("期望枚举类型名");
        }

        // 可选的泛型参数 [T, E]
        if (builder.getTokenType() == AsterTokenTypes.LBRACKET) {
            parseTypeParams(builder);
        }

        // 冒号和变体列表
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseVariantList(builder);
        } else {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.ENUM_DECL);
    }

    /**
     * 解析类型别名声明
     */
    private void parseTypeAliasDecl(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'type' 关键字
        builder.advanceLexer();

        // 别名名（支持 TYPE_IDENT 和 IDENT）
        if (builder.getTokenType() == AsterTokenTypes.TYPE_IDENT ||
            builder.getTokenType() == AsterTokenTypes.IDENT) {
            builder.advanceLexer();
        } else {
            builder.error("期望类型别名名");
        }

        // 可选的泛型参数 [T, E]
        if (builder.getTokenType() == AsterTokenTypes.LBRACKET) {
            parseTypeParams(builder);
        }

        // '=' 和目标类型（必须）
        if (builder.getTokenType() == AsterTokenTypes.EQUALS) {
            builder.advanceLexer();
            parseType(builder);
        } else {
            builder.error("期望 '='");
        }

        marker.done(AsterElementTypes.TYPE_ALIAS_DECL);
    }

    /**
     * 解析导入声明（自然语言语法）
     * 语法1: Use module.path.
     * 语法2: Use module.path as alias.
     * <p>
     * 支持换行和注释：
     * - Use module.path // comment
     *     as alias.
     * - Use module.path # comment
     *     as alias.
     */
    private void parseImportDecl(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'Use' 关键字
        builder.advanceLexer();

        // 模块路径（需要注意不要消耗 'as' 关键字或声明关键字）
        // 同时支持 IDENT 和 TYPE_IDENT
        int beforeOffset = builder.getCurrentOffset();
        while (builder.getTokenType() == AsterTokenTypes.IDENT ||
               builder.getTokenType() == AsterTokenTypes.TYPE_IDENT ||
               builder.getTokenType() == AsterTokenTypes.DOT) {
            String tokenText = builder.getTokenText();
            if (tokenText != null) {
                String lower = tokenText.toLowerCase();
                // 检查是否是 'as' 关键字或声明关键字
                if ("as".equals(lower) || isDeclarationKeyword(lower)) {
                    break;
                }
            }
            builder.advanceLexer();
        }

        // 检查是否解析了模块路径
        if (builder.getCurrentOffset() == beforeOffset) {
            builder.error("期望模块路径");
        }

        // 跳过换行和注释，然后检查 'as' 关键字
        skipNewlinesAndComments(builder);

        // 'as' 别名（可选）
        if ((builder.getTokenType() == AsterTokenTypes.IDENT ||
             builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) &&
            "as".equalsIgnoreCase(builder.getTokenText())) {
            builder.advanceLexer();
            // 跳过换行和注释
            skipNewlinesAndComments(builder);
            if (builder.getTokenType() == AsterTokenTypes.IDENT ||
                builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                builder.advanceLexer();
            } else {
                builder.error("期望别名");
            }
        }

        // 结束的句号（可选）
        if (builder.getTokenType() == AsterTokenTypes.DOT) {
            builder.advanceLexer();
        }

        marker.done(AsterElementTypes.IMPORT_DECL);
    }

    /**
     * 解析导入声明（传统语法，保留向后兼容）
     * 语法: import module.path as alias
     * <p>
     * 支持换行和注释：
     * - import module.path // comment
     *     as alias
     */
    private void parseImportDeclLegacy(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'import' 关键字
        builder.advanceLexer();

        // 模块路径（同时支持 IDENT 和 TYPE_IDENT）
        // 但不要消耗 'as' 关键字或其他声明关键字
        int beforeOffset = builder.getCurrentOffset();
        while (builder.getTokenType() == AsterTokenTypes.IDENT ||
               builder.getTokenType() == AsterTokenTypes.TYPE_IDENT ||
               builder.getTokenType() == AsterTokenTypes.DOT) {
            String tokenText = builder.getTokenText();
            if (tokenText != null) {
                String lower = tokenText.toLowerCase();
                // 检查是否是 'as' 关键字或声明关键字
                if ("as".equals(lower) || isDeclarationKeyword(lower)) {
                    break;
                }
            }
            builder.advanceLexer();
        }

        // 检查是否解析了模块路径
        if (builder.getCurrentOffset() == beforeOffset) {
            builder.error("期望模块路径");
        }

        // 跳过换行和注释
        skipNewlinesAndComments(builder);

        // 'as' 别名
        if ((builder.getTokenType() == AsterTokenTypes.IDENT ||
             builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) &&
            "as".equalsIgnoreCase(builder.getTokenText())) {
            builder.advanceLexer();
            // 跳过换行和注释
            skipNewlinesAndComments(builder);
            if (builder.getTokenType() == AsterTokenTypes.IDENT ||
                builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                builder.advanceLexer();
            } else {
                builder.error("期望别名");
            }
        }

        marker.done(AsterElementTypes.IMPORT_DECL);
    }

    /**
     * 解析模块声明（自然语言语法）
     * 语法: This module is name.
     */
    private void parseModuleDecl(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'This' 关键字
        builder.advanceLexer();

        // 'module' 关键字（同时支持 IDENT 和 TYPE_IDENT）
        if ((builder.getTokenType() == AsterTokenTypes.IDENT ||
             builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) &&
            "module".equalsIgnoreCase(builder.getTokenText())) {
            builder.advanceLexer();
        } else {
            builder.error("期望 'module'");
        }

        // 'is' 关键字（同时支持 IDENT 和 TYPE_IDENT）
        if ((builder.getTokenType() == AsterTokenTypes.IDENT ||
             builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) &&
            "is".equalsIgnoreCase(builder.getTokenText())) {
            builder.advanceLexer();
        } else {
            builder.error("期望 'is'");
        }

        // 模块名（可以是点分隔的路径，支持 IDENT 和 TYPE_IDENT）
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
            while (builder.getTokenType() == AsterTokenTypes.DOT) {
                builder.advanceLexer();
                if (builder.getTokenType() == AsterTokenTypes.IDENT ||
                    builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                    builder.advanceLexer();
                } else {
                    builder.error("期望模块名");
                    break;
                }
            }
        } else {
            builder.error("期望模块名");
        }

        // 结束的句号（可选）
        if (builder.getTokenType() == AsterTokenTypes.DOT) {
            builder.advanceLexer();
        }

        marker.done(AsterElementTypes.MODULE_DECL);
    }

    /**
     * 解析模块声明（传统语法，保留向后兼容）
     * 语法: module name
     */
    private void parseModuleDeclLegacy(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'module' 关键字
        builder.advanceLexer();

        // 模块名（可以是点分隔的路径，支持 IDENT 和 TYPE_IDENT）
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
            while (builder.getTokenType() == AsterTokenTypes.DOT) {
                builder.advanceLexer();
                if (builder.getTokenType() == AsterTokenTypes.IDENT ||
                    builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                    builder.advanceLexer();
                } else {
                    builder.error("期望模块名");
                    break;
                }
            }
        } else {
            builder.error("期望模块名");
        }

        marker.done(AsterElementTypes.MODULE_DECL);
    }

    /**
     * 解析 capabilities 声明
     * 语法: capabilities:
     *         capability1
     *         capability2
     * <p>
     * 由于 Lexer 已过滤 INDENT/DEDENT，使用顶层关键词识别块边界。
     */
    private void parseCapabilitiesDecl(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'capabilities' 关键字
        builder.advanceLexer();

        // ':' 和能力列表
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            skipNewlines(builder);

            // 解析能力列表，直到遇到顶层关键词或文件结束
            while (!builder.eof() && !isTopLevelKeyword(builder)) {
                int beforeOffset = builder.getCurrentOffset();

                skipNewlines(builder);
                if (builder.eof() || isTopLevelKeyword(builder)) {
                    break;
                }

                // 消耗能力名称
                if (builder.getTokenType() == AsterTokenTypes.IDENT ||
                    builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                    builder.advanceLexer();
                } else if (builder.getTokenType() == AsterTokenTypes.COMMENT) {
                    builder.advanceLexer();
                } else {
                    advanceWithError(builder, "期望能力名称");
                }

                // 防止无限循环
                if (builder.getCurrentOffset() == beforeOffset && !builder.eof()) {
                    advanceWithError(builder, "意外的 token");
                }
            }
        } else {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.CAPABILITIES_DECL);
    }

    /**
     * 解析顶层 workflow 声明
     * 语法: workflow name(params) uses [cap1, cap2]:
     *         body
     */
    private void parseWorkflowDecl(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'workflow' 关键字
        builder.advanceLexer();

        // workflow 名称（支持 IDENT 和 TYPE_IDENT）
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
        } else {
            builder.error("期望 workflow 名称");
        }

        // 可选的参数列表 (...)
        if (builder.getTokenType() == AsterTokenTypes.LPAREN) {
            parseParameterList(builder);
        }

        // 可选的 uses 子句（支持 IDENT 和 TYPE_IDENT）
        if ((builder.getTokenType() == AsterTokenTypes.IDENT ||
             builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) &&
            "uses".equalsIgnoreCase(builder.getTokenText())) {
            parseUsesClause(builder);
        }

        // ':' 和 workflow 体
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseWorkflowBody(builder);
        } else {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.WORKFLOW_STMT);
    }

    /**
     * 解析 uses 子句
     * 语法: uses [cap1, cap2, ...]
     */
    private void parseUsesClause(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'uses' 关键字
        builder.advanceLexer();

        // '[' 能力列表 ']'
        if (builder.getTokenType() == AsterTokenTypes.LBRACKET) {
            builder.advanceLexer();

            while (!builder.eof() && builder.getTokenType() != AsterTokenTypes.RBRACKET) {
                int beforeOffset = builder.getCurrentOffset();
                skipNewlines(builder);

                if (builder.getTokenType() == AsterTokenTypes.RBRACKET) {
                    break;
                }

                if (builder.getTokenType() == AsterTokenTypes.IDENT ||
                    builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                    builder.advanceLexer();

                    // 支持能力参数：Cap["param"]、Cap[param1, param2] 或 Cap[name: Type]
                    if (builder.getTokenType() == AsterTokenTypes.LBRACKET) {
                        builder.advanceLexer();
                        while (!builder.eof() && builder.getTokenType() != AsterTokenTypes.RBRACKET) {
                            int paramBeforeOffset = builder.getCurrentOffset();
                            skipNewlines(builder);

                            if (builder.getTokenType() == AsterTokenTypes.RBRACKET) {
                                break;
                            }

                            // 解析参数：STRING | IDENT | IDENT : Type | IDENT = Expr
                            if (builder.getTokenType() == AsterTokenTypes.STRING) {
                                builder.advanceLexer();
                            } else if (builder.getTokenType() == AsterTokenTypes.IDENT ||
                                       builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                                builder.advanceLexer();
                                // 键值形式：name: Type 或 name = value
                                if (builder.getTokenType() == AsterTokenTypes.COLON) {
                                    builder.advanceLexer();
                                    parseType(builder);
                                } else if (builder.getTokenType() == AsterTokenTypes.EQUALS) {
                                    builder.advanceLexer();
                                    parseExpression(builder);
                                }
                            }

                            skipNewlines(builder);
                            if (builder.getTokenType() == AsterTokenTypes.COMMA) {
                                builder.advanceLexer();
                            } else if (builder.getTokenType() != AsterTokenTypes.RBRACKET) {
                                // 防止无限循环
                                if (builder.getCurrentOffset() == paramBeforeOffset && !builder.eof()) {
                                    advanceWithError(builder, "意外的 token");
                                }
                                break;
                            }
                        }
                        if (builder.getTokenType() == AsterTokenTypes.RBRACKET) {
                            builder.advanceLexer();
                        } else {
                            builder.error("期望 ']'");
                        }
                    }
                }

                skipNewlines(builder);
                if (builder.getTokenType() == AsterTokenTypes.COMMA) {
                    builder.advanceLexer();
                } else if (builder.getTokenType() != AsterTokenTypes.RBRACKET) {
                    // 防止无限循环
                    if (builder.getCurrentOffset() == beforeOffset && !builder.eof()) {
                        advanceWithError(builder, "意外的 token");
                    }
                    break;
                }
            }

            if (builder.getTokenType() == AsterTokenTypes.RBRACKET) {
                builder.advanceLexer();
            } else {
                builder.error("期望 ']'");
            }
        } else {
            builder.error("期望 '['");
        }

        marker.done(AsterElementTypes.USES_CLAUSE);
    }

    /**
     * 解析 workflow 体（支持 step、compensate、timeout、retry 以及常规语句）
     * <p>
     * 由于 Lexer 已过滤 INDENT/DEDENT，使用顶层关键词识别块边界。
     */
    private void parseWorkflowBody(PsiBuilder builder) {
        skipNewlines(builder);

        // 解析 workflow 体，直到遇到顶层关键词或文件结束
        while (!builder.eof() && !isTopLevelKeyword(builder)) {
            int beforeOffset = builder.getCurrentOffset();

            skipNewlines(builder);
            if (builder.eof() || isTopLevelKeyword(builder)) {
                break;
            }

            String text = builder.getTokenText();
            IElementType tokenType = builder.getTokenType();

            // 同时支持 IDENT 和 TYPE_IDENT（首字母大写的 Step、Compensate 等）
            if (tokenType == AsterTokenTypes.IDENT || tokenType == AsterTokenTypes.TYPE_IDENT) {
                if ("step".equalsIgnoreCase(text)) {
                    parseWorkflowStep(builder);
                } else if ("compensate".equalsIgnoreCase(text)) {
                    parseWorkflowCompensate(builder);
                } else if ("timeout".equalsIgnoreCase(text)) {
                    parseWorkflowTimeout(builder);
                } else if ("retry".equalsIgnoreCase(text)) {
                    parseWorkflowRetry(builder);
                } else {
                    // 常规语句（let, set, if, await, return 等）
                    parseStatement(builder);
                }
            } else if (tokenType == AsterTokenTypes.NEWLINE) {
                builder.advanceLexer();
            } else if (tokenType == AsterTokenTypes.COMMENT) {
                builder.advanceLexer();
            } else {
                // 其他表达式语句
                parseStatement(builder);
            }

            // 防止无限循环：如果没有前进，强制推进
            if (builder.getCurrentOffset() == beforeOffset && !builder.eof()) {
                advanceWithError(builder, "意外的 workflow 子句");
            }
        }
    }

    /**
     * 解析代码块
     * <p>
     * 由于 IntelliJ 要求 token 范围严格递增，Lexer 已过滤 INDENT/DEDENT。
     * 此方法使用 NEWLINE 和顶层关键词来识别块边界。
     */
    private void parseBlock(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 跳过 NEWLINE
        skipNewlines(builder);

        // 解析块内语句，直到遇到顶层关键词或文件结束
        while (!builder.eof() && !isTopLevelKeyword(builder)) {
            int beforeOffset = builder.getCurrentOffset();
            parseStatement(builder);

            // 防止无限循环
            if (builder.getCurrentOffset() == beforeOffset && !builder.eof()) {
                advanceWithError(builder, "意外的 token");
            }
        }

        marker.done(AsterElementTypes.BLOCK);
    }

    /**
     * 检查当前 token 是否是顶层声明关键词
     * 用于在没有 INDENT/DEDENT 的情况下识别块边界
     * 支持 IDENT 和 TYPE_IDENT（首字母大写的关键词如 This、Module）
     *
     * 注意：workflow 不在此列表中，因为它可以作为嵌套语句出现在函数体内。
     * 顶层 workflow 声明由主解析循环的 switch/case 直接分发处理。
     */
    private boolean isTopLevelKeyword(PsiBuilder builder) {
        IElementType tokenType = builder.getTokenType();
        String tokenText = builder.getTokenText();

        // 同时支持 IDENT 和 TYPE_IDENT（首字母大写的情况）
        if ((tokenType == AsterTokenTypes.IDENT || tokenType == AsterTokenTypes.TYPE_IDENT)
                && tokenText != null) {
            String lower = tokenText.toLowerCase();
            return switch (lower) {
                // 自然语言语法关键词
                case "this", "to", "define", "use" -> true;
                // 传统语法关键词（不含 workflow，因为它可嵌套在函数体内）
                case "module", "capabilities", "func", "data", "enum", "type", "import" -> true;
                default -> false;
            };
        }
        return false;
    }

    /**
     * 检查当前 token 是否是 if 语句的块终结符
     * 包括顶层关键词和 else/elif
     * 支持 IDENT 和 TYPE_IDENT（首字母大写的关键词如 Else、Elif）
     */
    private boolean isIfBlockTerminator(PsiBuilder builder) {
        if (isTopLevelKeyword(builder)) {
            return true;
        }
        IElementType tokenType = builder.getTokenType();
        String tokenText = builder.getTokenText();
        // 同时支持 IDENT 和 TYPE_IDENT（首字母大写的情况）
        if ((tokenType == AsterTokenTypes.IDENT || tokenType == AsterTokenTypes.TYPE_IDENT)
                && tokenText != null) {
            String lower = tokenText.toLowerCase();
            return "else".equals(lower) || "elif".equals(lower);
        }
        return false;
    }

    /**
     * 解析语句
     * <p>
     * 支持大小写不敏感的关键词匹配（如 Let/let、Return/return）
     */
    private void parseStatement(PsiBuilder builder) {
        skipWhitespaceAndNewlines(builder);

        if (builder.eof() || isTopLevelKeyword(builder)) {
            return;
        }

        String tokenText = builder.getTokenText();
        IElementType tokenType = builder.getTokenType();

        // 根据标识符文本识别语句类型（支持大小写不敏感）
        if (tokenType == AsterTokenTypes.IDENT && tokenText != null) {
            switch (tokenText.toLowerCase()) {
                case "let" -> parseLetStmt(builder);
                case "set" -> parseSetStmt(builder);
                case "return" -> parseReturnStmt(builder);
                case "if" -> parseIfStmt(builder);
                case "match" -> parseMatchStmt(builder);
                case "start" -> parseStartStmt(builder);
                case "wait" -> parseWaitStmt(builder);
                case "workflow" -> parseWorkflowStmt(builder);
                case "for" -> parseForStmt(builder);
                case "while" -> parseWhileStmt(builder);
                case "it" -> parseItPerformsStmt(builder);
                default -> parseExpressionStmt(builder);
            }
        } else if (tokenType == AsterTokenTypes.TYPE_IDENT && tokenText != null) {
            // 支持以大写开头的关键词（如 Let、Return、If、Match 等）
            // 必须与 IDENT 分支保持完全一致
            switch (tokenText.toLowerCase()) {
                case "let" -> parseLetStmt(builder);
                case "set" -> parseSetStmt(builder);
                case "return" -> parseReturnStmt(builder);
                case "if" -> parseIfStmt(builder);
                case "match" -> parseMatchStmt(builder);
                case "start" -> parseStartStmt(builder);
                case "wait" -> parseWaitStmt(builder);
                case "workflow" -> parseWorkflowStmt(builder);
                case "for" -> parseForStmt(builder);
                case "while" -> parseWhileStmt(builder);
                case "it" -> parseItPerformsStmt(builder);
                default -> parseExpressionStmt(builder);
            }
        } else if (tokenType == AsterTokenTypes.COMMENT) {
            builder.advanceLexer();
        } else {
            parseExpressionStmt(builder);
        }
    }

    /**
     * 解析 let 语句
     */
    private void parseLetStmt(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'let' 关键字
        builder.advanceLexer();

        // 变量名（支持 IDENT 和 TYPE_IDENT）
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
        } else {
            builder.error("期望变量名");
        }

        // '=' 和表达式
        if (builder.getTokenType() == AsterTokenTypes.EQUALS) {
            builder.advanceLexer();
            parseExpression(builder);
        } else {
            builder.error("期望 '='");
        }

        marker.done(AsterElementTypes.LET_STMT);
    }

    /**
     * 解析 set 语句
     * 支持 set x = expr 和 set obj.field = expr
     */
    private void parseSetStmt(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'set' 关键字
        builder.advanceLexer();

        // 左值：可以是标识符或成员访问表达式 (obj.field)
        // 支持 IDENT 和 TYPE_IDENT
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            parseAssignTarget(builder);
        } else {
            builder.error("期望变量名");
        }

        // '=' 和表达式（必须）
        if (builder.getTokenType() == AsterTokenTypes.EQUALS) {
            builder.advanceLexer();
            parseExpression(builder);
        } else {
            builder.error("期望 '='");
        }

        marker.done(AsterElementTypes.SET_STMT);
    }

    /**
     * 解析赋值目标（左值）
     * 支持 x, obj.field, obj.a.b 等形式
     */
    private void parseAssignTarget(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 首个标识符
        builder.advanceLexer();

        // 处理成员访问链 .field.field...（支持 IDENT 和 TYPE_IDENT）
        boolean hasMember = false;
        while (builder.getTokenType() == AsterTokenTypes.DOT) {
            hasMember = true;
            builder.advanceLexer();
            if (builder.getTokenType() == AsterTokenTypes.IDENT ||
                builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                builder.advanceLexer();
            } else {
                builder.error("期望成员名");
                break;
            }
        }

        // 如果有成员访问，生成 MEMBER_EXPR；否则生成 NAME_EXPR
        if (hasMember) {
            marker.done(AsterElementTypes.MEMBER_EXPR);
        } else {
            marker.done(AsterElementTypes.NAME_EXPR);
        }
    }

    /**
     * 解析 return 语句
     */
    private void parseReturnStmt(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'return' 关键字
        builder.advanceLexer();

        // 返回表达式（可选）
        if (!builder.eof() &&
            builder.getTokenType() != AsterTokenTypes.NEWLINE &&
            builder.getTokenType() != AsterTokenTypes.DEDENT) {
            parseExpression(builder);
        }

        marker.done(AsterElementTypes.RETURN_STMT);
    }

    /**
     * 解析 if 语句
     */
    private void parseIfStmt(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'if' 关键字
        builder.advanceLexer();

        // 条件表达式
        parseExpression(builder);

        // ':' 和 then 块（冒号必须）
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseIfBlock(builder);
        } else {
            builder.error("期望 ':'");
        }

        // 处理 elif/else 分支（可选，支持链式）
        while (!builder.eof()) {
            skipNewlines(builder);
            // 跳过分支之间的注释
            while (builder.getTokenType() == AsterTokenTypes.COMMENT) {
                builder.advanceLexer();
                skipNewlines(builder);
            }
            String tokenText = builder.getTokenText();
            IElementType tokenType = builder.getTokenType();
            // 同时支持 IDENT 和 TYPE_IDENT（首字母大写的 Elif、Else）
            if ((tokenType == AsterTokenTypes.IDENT || tokenType == AsterTokenTypes.TYPE_IDENT)
                    && tokenText != null) {
                String lower = tokenText.toLowerCase();
                if ("elif".equals(lower)) {
                    // elif 分支
                    builder.advanceLexer();
                    parseExpression(builder);
                    if (builder.getTokenType() == AsterTokenTypes.COLON) {
                        builder.advanceLexer();
                        parseIfBlock(builder);
                    } else {
                        builder.error("期望 ':'");
                    }
                } else if ("else".equals(lower)) {
                    // else 分支
                    builder.advanceLexer();
                    if (builder.getTokenType() == AsterTokenTypes.COLON) {
                        builder.advanceLexer();
                        parseBlock(builder);
                    } else {
                        builder.error("期望 ':'");
                    }
                    break; // else 是最后一个分支
                } else {
                    break; // 不是 elif/else，退出
                }
            } else {
                break;
            }
        }

        marker.done(AsterElementTypes.IF_STMT);
    }

    /**
     * 解析 if 语句的块体
     * 与普通 block 不同，遇到 else/elif 时也会终止
     */
    private void parseIfBlock(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 跳过 NEWLINE
        skipNewlines(builder);

        // 解析块内语句，直到遇到 if 块终结符（顶层关键词或 else/elif）
        while (!builder.eof() && !isIfBlockTerminator(builder)) {
            int beforeOffset = builder.getCurrentOffset();
            parseStatement(builder);

            // 防止无限循环
            if (builder.getCurrentOffset() == beforeOffset && !builder.eof()) {
                advanceWithError(builder, "意外的 token");
            }
        }

        marker.done(AsterElementTypes.BLOCK);
    }

    /**
     * 解析 match 语句
     */
    private void parseMatchStmt(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'match' 关键字
        builder.advanceLexer();

        // 匹配表达式
        parseExpression(builder);

        // ':' 和 cases（冒号必须）
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseMatchCases(builder);
        } else {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.MATCH_STMT);
    }

    /**
     * 解析 match cases
     * <p>
     * 由于 Lexer 已过滤 INDENT/DEDENT，使用顶层关键词识别块边界。
     */
    private void parseMatchCases(PsiBuilder builder) {
        skipNewlines(builder);

        // 解析 match cases，直到遇到顶层关键词或文件结束
        while (!builder.eof() && !isTopLevelKeyword(builder)) {
            int beforeOffset = builder.getCurrentOffset();

            // 跳过注释
            if (builder.getTokenType() == AsterTokenTypes.COMMENT) {
                builder.advanceLexer();
                skipNewlines(builder);
                continue;
            }

            // 检查是否是 case 模式起始（非语句关键词的标识符）
            if (isMatchCasePattern(builder)) {
                parseMatchCase(builder);
            } else {
                // 不是有效的 case 模式，跳出
                break;
            }
            skipNewlines(builder);

            // 防止无限循环
            if (builder.getCurrentOffset() == beforeOffset && !builder.eof()) {
                advanceWithError(builder, "意外的 token");
            }
        }
    }

    /**
     * 检查当前位置是否是 match case 模式
     * case 模式通常是: 标识符/类型 -> 表达式
     */
    private boolean isMatchCasePattern(PsiBuilder builder) {
        IElementType tokenType = builder.getTokenType();
        String tokenText = builder.getTokenText();

        // case 模式可以是标识符或类型标识符
        if (tokenType == AsterTokenTypes.IDENT || tokenType == AsterTokenTypes.TYPE_IDENT) {
            if (tokenText != null) {
                String lower = tokenText.toLowerCase();
                // 排除语句关键词 - 这些不是 case 模式
                return switch (lower) {
                    case "let", "set", "return", "if", "match", "for", "while", "start", "wait", "it", "workflow" -> false;
                    default -> true;
                };
            }
        }
        return false;
    }

    /**
     * 解析单个 match case
     * 语法: Pattern -> Expression 或 Pattern -> return Expression
     * case body 在箭头后同一行解析，换行符作为 case 边界
     */
    private void parseMatchCase(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 模式
        parseMatchPattern(builder);

        // '->' 箭头符号
        if (builder.getTokenType() == AsterTokenTypes.MINUS) {
            builder.advanceLexer();
            if (builder.getTokenType() == AsterTokenTypes.GT) {
                builder.advanceLexer();
            } else {
                builder.error("期望 '>'（箭头的一部分）");
            }
        } else {
            builder.error("期望 '->'");
        }

        // case body - 箭头后同一行的内容
        // 只跳过空白，不跳过换行（换行是 case 边界）
        skipWhitespace(builder);

        // 如果不是换行、EOF、顶层关键词，则解析 body
        if (!builder.eof()
                && builder.getTokenType() != AsterTokenTypes.NEWLINE
                && !isTopLevelKeyword(builder)) {
            String tokenText = builder.getTokenText();
            IElementType tokenType = builder.getTokenType();
            // return 语句特殊处理
            if ((tokenType == AsterTokenTypes.IDENT || tokenType == AsterTokenTypes.TYPE_IDENT)
                    && tokenText != null && "return".equalsIgnoreCase(tokenText)) {
                parseReturnStmt(builder);
            } else {
                parseExpression(builder);
            }
        }

        marker.done(AsterElementTypes.MATCH_CASE);
    }

    /**
     * 解析 match 模式（专门用于 case 模式）
     * 模式解析在遇到箭头 -> 时终止
     */
    private void parseMatchPattern(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 解析模式直到遇到箭头、换行或顶层关键词
        while (!builder.eof()) {
            IElementType type = builder.getTokenType();
            // 遇到箭头时停止（保留给 case 解析）
            if (type == AsterTokenTypes.MINUS) {
                // 检查是否是 -> 箭头
                IElementType next = builder.lookAhead(1);
                if (next == AsterTokenTypes.GT) {
                    break;
                }
            }
            if (type == AsterTokenTypes.NEWLINE || isTopLevelKeyword(builder)) {
                break;
            }
            builder.advanceLexer();
        }

        marker.done(AsterElementTypes.PATTERN);
    }

    /**
     * 解析 start 语句
     */
    private void parseStartStmt(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'start' 关键字
        builder.advanceLexer();

        // 任务名（支持 IDENT 和 TYPE_IDENT）
        boolean hasName = false;
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
            hasName = true;
        } else {
            builder.error("期望任务名");
        }

        // '=' 和表达式
        if (builder.getTokenType() == AsterTokenTypes.EQUALS) {
            builder.advanceLexer();
            parseExpression(builder);
        } else if (hasName) {
            builder.error("期望 '='");
        }

        marker.done(AsterElementTypes.START_STMT);
    }

    /**
     * 解析 wait 语句
     */
    private void parseWaitStmt(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'wait' 关键字
        builder.advanceLexer();

        // 任务名列表（支持 IDENT 和 TYPE_IDENT）
        int beforeOffset = builder.getCurrentOffset();
        while (builder.getTokenType() == AsterTokenTypes.IDENT ||
               builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
            if (builder.getTokenType() == AsterTokenTypes.COMMA) {
                builder.advanceLexer();
            } else {
                break;
            }
        }

        // 检查是否至少解析了一个任务名
        if (builder.getCurrentOffset() == beforeOffset) {
            builder.error("期望任务名");
        }

        marker.done(AsterElementTypes.WAIT_STMT);
    }

    /**
     * 解析 for 循环语句
     * 语法: for item in collection:
     *         body
     */
    private void parseForStmt(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'for' 关键字
        builder.advanceLexer();

        // 循环变量（支持 IDENT 和 TYPE_IDENT）
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
        } else {
            builder.error("期望循环变量名");
        }

        // 'in' 关键字（支持 IDENT 和 TYPE_IDENT）
        if ((builder.getTokenType() == AsterTokenTypes.IDENT ||
             builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) &&
            "in".equalsIgnoreCase(builder.getTokenText())) {
            builder.advanceLexer();
        } else {
            builder.error("期望 'in'");
        }

        // 集合表达式
        parseExpression(builder);

        // ':' 和循环体
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseBlock(builder);
        } else {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.FOR_STMT);
    }

    /**
     * 解析 while 循环语句
     * 语法: while condition:
     *         body
     */
    private void parseWhileStmt(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'while' 关键字
        builder.advanceLexer();

        // 条件表达式
        parseExpression(builder);

        // ':' 和循环体
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseBlock(builder);
        } else {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.WHILE_STMT);
    }

    /**
     * 解析 "It performs" 自然语言工作流语句
     * 语法: It performs action.
     * 或: It performs action with param: value.
     */
    private void parseItPerformsStmt(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'It' 关键字
        builder.advanceLexer();

        // 'performs' 关键字（支持 IDENT 和 TYPE_IDENT）
        if ((builder.getTokenType() == AsterTokenTypes.IDENT ||
             builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) &&
            "performs".equalsIgnoreCase(builder.getTokenText())) {
            builder.advanceLexer();
        } else {
            builder.error("期望 'performs'");
        }

        // 动作名称（支持 IDENT 和 TYPE_IDENT）
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
        } else {
            builder.error("期望动作名称");
        }

        // 可选的 'with' 参数子句（支持 IDENT 和 TYPE_IDENT）
        if ((builder.getTokenType() == AsterTokenTypes.IDENT ||
             builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) &&
            "with".equalsIgnoreCase(builder.getTokenText())) {
            builder.advanceLexer();
            parseNaturalParameterList(builder);
        }

        // 结束的句号（可选）
        if (builder.getTokenType() == AsterTokenTypes.DOT) {
            builder.advanceLexer();
        }

        marker.done(AsterElementTypes.IT_PERFORMS_STMT);
    }

    /**
     * 解析 workflow 语句（函数内部使用）
     * 语法: workflow name(params) uses [caps]:
     *         body
     * 或简单形式: workflow:
     *         body
     */
    private void parseWorkflowStmt(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'workflow' 关键字
        builder.advanceLexer();

        // 可选的 workflow 名称（支持 IDENT 和 TYPE_IDENT）
        if ((builder.getTokenType() == AsterTokenTypes.IDENT ||
             builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) &&
            !":".equals(builder.getTokenText())) {
            builder.advanceLexer();
        }

        // 可选的参数列表 (...)
        if (builder.getTokenType() == AsterTokenTypes.LPAREN) {
            parseParameterList(builder);
        }

        // 可选的 uses 子句（支持 IDENT 和 TYPE_IDENT）
        if ((builder.getTokenType() == AsterTokenTypes.IDENT ||
             builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) &&
            "uses".equalsIgnoreCase(builder.getTokenText())) {
            parseUsesClause(builder);
        }

        // ':' 和 workflow 体
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseWorkflowBody(builder);
        } else {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.WORKFLOW_STMT);
    }

    /**
     * 解析 workflow compensate 节点
     * 支持 compensate: 和 compensate stepName: 两种形式
     */
    private void parseWorkflowCompensate(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'compensate' 关键字
        builder.advanceLexer();

        // 可选的目标步骤名（compensate stepName:）（支持 IDENT 和 TYPE_IDENT）
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
        }

        // ':' 和补偿体
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseBlock(builder);
        } else {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.WORKFLOW_COMPENSATE);
    }

    /**
     * 解析 workflow timeout 节点
     * 支持单行语法: timeout: 30 seconds
     * 和块语法: timeout:
     *             duration = 30
     */
    private void parseWorkflowTimeout(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'timeout' 关键字
        builder.advanceLexer();

        // ':' 和超时配置
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();

            // 检查是单行还是块语法
            if (builder.getTokenType() == AsterTokenTypes.NEWLINE) {
                // 块语法
                parseBlock(builder);
            } else if (!builder.eof() &&
                       builder.getTokenType() != AsterTokenTypes.DEDENT) {
                // 单行语法：解析表达式直到行尾
                parseInlineConfigValues(builder);
            }
        } else {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.WORKFLOW_TIMEOUT);
    }

    /**
     * 解析 workflow retry 节点
     * 支持单行语法: retry: 3
     * 和块语法: retry:
     *             max_attempts = 3
     */
    private void parseWorkflowRetry(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'retry' 关键字
        builder.advanceLexer();

        // ':' 和重试配置
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();

            // 检查是单行还是块语法
            if (builder.getTokenType() == AsterTokenTypes.NEWLINE) {
                // 块语法
                parseBlock(builder);
            } else if (!builder.eof() &&
                       builder.getTokenType() != AsterTokenTypes.DEDENT) {
                // 单行语法：解析表达式直到行尾
                parseInlineConfigValues(builder);
            }
        } else {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.WORKFLOW_RETRY);
    }

    /**
     * 解析单行配置值（用于 timeout: 30 seconds 这样的语法）
     */
    private void parseInlineConfigValues(PsiBuilder builder) {
        while (!builder.eof() &&
               builder.getTokenType() != AsterTokenTypes.NEWLINE &&
               builder.getTokenType() != AsterTokenTypes.DEDENT) {
            builder.advanceLexer();
        }
    }

    /**
     * 解析单个 workflow step
     * 支持以下形式:
     * - step name:
     * - step name depends on dep1, dep2:
     * - step name depends on ["dep1", "dep2"]:
     */
    private void parseWorkflowStep(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 'step' 关键字
        builder.advanceLexer();

        // 步骤名（支持 IDENT 和 TYPE_IDENT）
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
        } else {
            builder.error("期望步骤名");
        }

        // 可选的 'depends on' 子句（支持 IDENT 和 TYPE_IDENT）
        if ((builder.getTokenType() == AsterTokenTypes.IDENT ||
             builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) &&
            "depends".equalsIgnoreCase(builder.getTokenText())) {
            builder.advanceLexer(); // 消耗 'depends'

            // 期望 'on'（支持 IDENT 和 TYPE_IDENT）
            if ((builder.getTokenType() == AsterTokenTypes.IDENT ||
                 builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) &&
                "on".equalsIgnoreCase(builder.getTokenText())) {
                builder.advanceLexer(); // 消耗 'on'

                // 解析依赖列表：支持列表语法 [...] 或逗号分隔的标识符
                if (builder.getTokenType() == AsterTokenTypes.LBRACKET) {
                    // 列表语法: ["step1", "step2"]
                    builder.advanceLexer();
                    int depCount = 0;
                    while (!builder.eof() && builder.getTokenType() != AsterTokenTypes.RBRACKET) {
                        int beforeOffset = builder.getCurrentOffset();
                        skipNewlines(builder);
                        // 支持 STRING、IDENT 和 TYPE_IDENT
                        if (builder.getTokenType() == AsterTokenTypes.STRING ||
                            builder.getTokenType() == AsterTokenTypes.IDENT ||
                            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                            builder.advanceLexer();
                            depCount++;
                        }
                        skipNewlines(builder);
                        if (builder.getTokenType() == AsterTokenTypes.COMMA) {
                            builder.advanceLexer();
                        } else if (builder.getTokenType() != AsterTokenTypes.RBRACKET) {
                            // 防止无限循环
                            if (builder.getCurrentOffset() == beforeOffset && !builder.eof()) {
                                advanceWithError(builder, "意外的 token");
                            }
                            break;
                        }
                    }
                    if (depCount == 0) {
                        builder.error("期望至少一个依赖项");
                    }
                    if (builder.getTokenType() == AsterTokenTypes.RBRACKET) {
                        builder.advanceLexer();
                    } else {
                        builder.error("期望 ']'");
                    }
                } else {
                    // 逗号分隔的标识符: dep1, dep2（支持 IDENT 和 TYPE_IDENT）
                    int depCount = 0;
                    while (builder.getTokenType() == AsterTokenTypes.IDENT ||
                           builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                        builder.advanceLexer();
                        depCount++;
                        if (builder.getTokenType() == AsterTokenTypes.COMMA) {
                            builder.advanceLexer();
                        } else {
                            break;
                        }
                    }
                    if (depCount == 0) {
                        builder.error("期望依赖项");
                    }
                }
            } else {
                builder.error("期望 'on'");
            }
        }

        // ':' 和步骤体（冒号必须）
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseBlock(builder);
        } else {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.WORKFLOW_STEP);
    }

    /**
     * 解析表达式语句
     */
    private void parseExpressionStmt(PsiBuilder builder) {
        parseExpression(builder);
    }

    /**
     * 解析表达式（带 PSI 节点创建）
     * 使用 Pratt 解析器处理运算符优先级
     */
    private void parseExpression(PsiBuilder builder) {
        parseExpressionWithPrecedence(builder, 0);
    }

    /**
     * 获取运算符优先级
     * 返回 -1 表示不是中缀运算符
     */
    private int getOperatorPrecedence(IElementType type) {
        // 比较运算符（最低优先级）
        if (type == AsterTokenTypes.LT || type == AsterTokenTypes.GT ||
            type == AsterTokenTypes.LTE || type == AsterTokenTypes.GTE ||
            type == AsterTokenTypes.NEQ) {
            return 1;
        }
        // 相等运算符 (==)
        if (type == AsterTokenTypes.EQUALS) {
            // 需要检查是否是 == 而不是赋值 =
            return 1;
        }
        // 加减
        if (type == AsterTokenTypes.PLUS || type == AsterTokenTypes.MINUS) {
            return 2;
        }
        // 乘除
        if (type == AsterTokenTypes.STAR || type == AsterTokenTypes.SLASH) {
            return 3;
        }
        return -1;
    }

    /**
     * 检查是否是 == 运算符（两个连续的 =）
     */
    private boolean isDoubleEquals(PsiBuilder builder) {
        if (builder.getTokenType() != AsterTokenTypes.EQUALS) {
            return false;
        }
        // 使用 lookAhead 检查下一个 token
        IElementType next = builder.lookAhead(1);
        return next == AsterTokenTypes.EQUALS;
    }

    /**
     * 使用 Pratt 解析器解析表达式
     */
    private void parseExpressionWithPrecedence(PsiBuilder builder, int minPrecedence) {
        // 解析左操作数（主表达式 + 后缀操作）
        PsiBuilder.Marker leftMarker = builder.mark();
        parsePostfixExpression(builder);

        // 处理中缀运算符
        while (!builder.eof()) {
            IElementType opType = builder.getTokenType();

            // 检查是否是 == 运算符
            boolean isEqEq = isDoubleEquals(builder);
            int precedence = isEqEq ? 1 : getOperatorPrecedence(opType);

            // 如果不是运算符或优先级不够，退出
            if (precedence < 0 || precedence < minPrecedence) {
                break;
            }

            // 如果是单个 = 且不是 ==，可能是赋值，不作为中缀运算处理
            if (opType == AsterTokenTypes.EQUALS && !isEqEq) {
                break;
            }

            // 消耗运算符
            if (isEqEq) {
                builder.advanceLexer(); // 第一个 =
                builder.advanceLexer(); // 第二个 =
            } else {
                builder.advanceLexer();
            }

            // 解析右操作数（更高优先级）
            parseExpressionWithPrecedence(builder, precedence + 1);

            // 完成二元表达式节点
            leftMarker.done(AsterElementTypes.BINARY_EXPR);
            leftMarker = leftMarker.precede();
        }

        // 丢弃最外层未使用的标记
        leftMarker.drop();
    }

    /**
     * 解析后缀表达式（主表达式 + 调用/成员访问/索引）
     */
    private void parsePostfixExpression(PsiBuilder builder) {
        PsiBuilder.Marker exprMarker = builder.mark();
        parsePrimaryExpression(builder);

        // 处理后缀操作
        while (!builder.eof()) {
            IElementType type = builder.getTokenType();

            if (type == AsterTokenTypes.LPAREN) {
                // 函数调用
                parseArgumentList(builder);
                exprMarker.done(AsterElementTypes.CALL_EXPR);
                exprMarker = exprMarker.precede();
            } else if (type == AsterTokenTypes.DOT) {
                // 成员访问
                builder.advanceLexer();
                if (builder.getTokenType() == AsterTokenTypes.IDENT ||
                    builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                    builder.advanceLexer();
                } else {
                    builder.error("期望成员名");
                }
                exprMarker.done(AsterElementTypes.MEMBER_EXPR);
                exprMarker = exprMarker.precede();
            } else if (type == AsterTokenTypes.LBRACKET) {
                // 泛型参数或索引
                parseTypeArgs(builder);
            } else {
                break;
            }
        }

        exprMarker.drop();
    }

    /**
     * 解析主表达式
     */
    private void parsePrimaryExpression(PsiBuilder builder) {
        IElementType type = builder.getTokenType();
        String text = builder.getTokenText();

        if (type == AsterTokenTypes.IDENT) {
            // 布尔字面量优先检查
            if ("true".equals(text) || "false".equals(text)) {
                PsiBuilder.Marker marker = builder.mark();
                builder.advanceLexer();
                marker.done(AsterElementTypes.LITERAL_EXPR);
            // 检查特殊关键字表达式
            } else if ("Some".equals(text)) {
                parseSomeExpression(builder);
            } else if ("None".equals(text)) {
                parseNoneExpression(builder);
            } else if ("Ok".equals(text)) {
                parseOkExpression(builder);
            } else if ("Err".equals(text)) {
                parseErrExpression(builder);
            } else if ("await".equals(text)) {
                parseAwaitExpression(builder);
            } else {
                // 普通名称表达式
                PsiBuilder.Marker marker = builder.mark();
                builder.advanceLexer();
                marker.done(AsterElementTypes.NAME_EXPR);
            }
        } else if (type == AsterTokenTypes.TYPE_IDENT) {
            // 检查 Option/Result 关键字（大写开头会被词法器标记为 TYPE_IDENT）
            if ("Some".equals(text)) {
                parseSomeExpression(builder);
            } else if ("None".equals(text)) {
                parseNoneExpression(builder);
            } else if ("Ok".equals(text)) {
                parseOkExpression(builder);
            } else if ("Err".equals(text)) {
                parseErrExpression(builder);
            } else {
                // 普通类型构造表达式
                parseConstructExpression(builder);
            }
        } else if (type == AsterTokenTypes.INT ||
                   type == AsterTokenTypes.FLOAT ||
                   type == AsterTokenTypes.STRING ||
                   type == AsterTokenTypes.LONG ||
                   type == AsterTokenTypes.BOOL ||
                   type == AsterTokenTypes.NULL) {
            // 字面量（包括布尔和 null）
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            marker.done(AsterElementTypes.LITERAL_EXPR);
        } else if (type == AsterTokenTypes.LBRACKET) {
            // 列表字面量
            parseListLiteral(builder);
        } else if (type == AsterTokenTypes.LPAREN) {
            // 括号表达式或 Lambda
            parseParenOrLambda(builder);
        } else {
            // 跳过不认识的 token（防止卡死）
            if (!isExpressionTerminator(type)) {
                builder.advanceLexer();
            }
        }
    }

    /**
     * 判断是否为表达式终结符
     */
    private boolean isExpressionTerminator(IElementType type) {
        return type == AsterTokenTypes.NEWLINE ||
               type == AsterTokenTypes.DEDENT ||
               type == AsterTokenTypes.COLON ||
               type == AsterTokenTypes.RPAREN ||
               type == AsterTokenTypes.RBRACKET ||
               type == AsterTokenTypes.COMMA;
    }

    /**
     * 解析参数列表（用于函数调用）
     */
    private void parseArgumentList(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // '('
        builder.advanceLexer();

        // 参数列表（支持多行调用）
        while (!builder.eof() && builder.getTokenType() != AsterTokenTypes.RPAREN) {
            skipNewlines(builder);

            if (builder.getTokenType() == AsterTokenTypes.RPAREN) {
                break;
            }

            int beforeOffset = builder.getCurrentOffset();
            parseExpression(builder);

            // 零宽保护：防止无限循环
            if (builder.getCurrentOffset() == beforeOffset && !builder.eof()) {
                advanceWithError(builder, "意外的 token");
                continue;
            }

            skipNewlines(builder);
            if (builder.getTokenType() == AsterTokenTypes.COMMA) {
                builder.advanceLexer();
            } else if (builder.getTokenType() != AsterTokenTypes.RPAREN) {
                break;
            }
        }

        // ')'
        if (builder.getTokenType() == AsterTokenTypes.RPAREN) {
            builder.advanceLexer();
        } else {
            builder.error("期望 ')'");
        }

        marker.done(AsterElementTypes.ARGUMENT_LIST);
    }

    /**
     * 解析 Some 表达式
     */
    private void parseSomeExpression(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // 'Some'

        if (builder.getTokenType() == AsterTokenTypes.LPAREN) {
            parseArgumentList(builder);
        }

        marker.done(AsterElementTypes.SOME_EXPR);
    }

    /**
     * 解析 None 表达式
     */
    private void parseNoneExpression(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // 'None'
        marker.done(AsterElementTypes.NONE_EXPR);
    }

    /**
     * 解析 Ok 表达式
     */
    private void parseOkExpression(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // 'Ok'

        if (builder.getTokenType() == AsterTokenTypes.LPAREN) {
            parseArgumentList(builder);
        }

        marker.done(AsterElementTypes.OK_EXPR);
    }

    /**
     * 解析 Err 表达式
     */
    private void parseErrExpression(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // 'Err'

        if (builder.getTokenType() == AsterTokenTypes.LPAREN) {
            parseArgumentList(builder);
        }

        marker.done(AsterElementTypes.ERR_EXPR);
    }

    /**
     * 解析 await 表达式
     */
    private void parseAwaitExpression(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // 'await'
        parseExpression(builder);
        marker.done(AsterElementTypes.AWAIT_EXPR);
    }

    /**
     * 解析构造表达式
     * <p>
     * Aster 中数据类型构造使用 Type(field1 = value1, field2 = value2) 语法
     */
    private void parseConstructExpression(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 类型名
        builder.advanceLexer();

        // 泛型参数（可选）
        if (builder.getTokenType() == AsterTokenTypes.LBRACKET) {
            parseTypeArgs(builder);
        }

        // 构造字段 (...)
        if (builder.getTokenType() == AsterTokenTypes.LPAREN) {
            parseConstructFields(builder);
        }

        marker.done(AsterElementTypes.CONSTRUCT_EXPR);
    }

    /**
     * 解析构造字段列表
     */
    private void parseConstructFields(PsiBuilder builder) {
        // '('
        builder.advanceLexer();

        while (!builder.eof() && builder.getTokenType() != AsterTokenTypes.RPAREN) {
            skipWhitespaceAndNewlines(builder);

            if (builder.getTokenType() == AsterTokenTypes.RPAREN) {
                break;
            }

            // 检查是否是命名字段 (name = value) 还是位置参数
            int beforeExprOffset = builder.getCurrentOffset();
            if (builder.getTokenType() == AsterTokenTypes.IDENT &&
                builder.lookAhead(1) == AsterTokenTypes.EQUALS) {
                // 命名字段: name = value
                PsiBuilder.Marker fieldMarker = builder.mark();
                builder.advanceLexer(); // 字段名
                builder.advanceLexer(); // '='
                int afterEqualsOffset = builder.getCurrentOffset();
                parseExpression(builder);
                // 检查是否解析了表达式
                if (builder.getCurrentOffset() == afterEqualsOffset) {
                    builder.error("期望表达式");
                }
                fieldMarker.done(AsterElementTypes.CONSTRUCT_FIELD);
            } else {
                // 位置参数：作为表达式处理
                parseExpression(builder);
            }
            // 防止无限循环
            if (builder.getCurrentOffset() == beforeExprOffset && !builder.eof()) {
                advanceWithError(builder, "意外的 token");
                continue;
            }

            skipWhitespaceAndNewlines(builder);
            if (builder.getTokenType() == AsterTokenTypes.COMMA) {
                builder.advanceLexer();
            } else if (builder.getTokenType() != AsterTokenTypes.RPAREN) {
                // 不是逗号也不是闭合括号，退出循环
                break;
            }
        }

        // ')'
        if (builder.getTokenType() == AsterTokenTypes.RPAREN) {
            builder.advanceLexer();
        } else {
            builder.error("期望 ')'");
        }
    }

    /**
     * 解析列表字面量
     */
    private void parseListLiteral(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // '['
        builder.advanceLexer();

        while (!builder.eof() && builder.getTokenType() != AsterTokenTypes.RBRACKET) {
            skipNewlines(builder);

            if (builder.getTokenType() == AsterTokenTypes.RBRACKET) {
                break;
            }

            int beforeOffset = builder.getCurrentOffset();
            parseExpression(builder);

            // 零宽保护：防止无限循环
            if (builder.getCurrentOffset() == beforeOffset && !builder.eof()) {
                advanceWithError(builder, "意外的 token");
                continue;
            }

            skipNewlines(builder);
            if (builder.getTokenType() == AsterTokenTypes.COMMA) {
                builder.advanceLexer();
            } else if (builder.getTokenType() != AsterTokenTypes.RBRACKET) {
                break;
            }
        }

        // ']'
        if (builder.getTokenType() == AsterTokenTypes.RBRACKET) {
            builder.advanceLexer();
        } else {
            builder.error("期望 ']'");
        }

        marker.done(AsterElementTypes.LIST_LITERAL_EXPR);
    }

    /**
     * 解析括号表达式或 Lambda
     */
    private void parseParenOrLambda(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // '('
        builder.advanceLexer();

        // 检查是否为 Lambda: (x, y) ->
        // 简化处理：先消费括号内容
        while (!builder.eof() && builder.getTokenType() != AsterTokenTypes.RPAREN) {
            skipNewlines(builder);

            if (builder.getTokenType() == AsterTokenTypes.RPAREN) {
                break;
            }

            int beforeOffset = builder.getCurrentOffset();
            parseExpression(builder);

            // 零宽保护
            if (builder.getCurrentOffset() == beforeOffset && !builder.eof()) {
                advanceWithError(builder, "意外的 token");
                continue;
            }

            skipNewlines(builder);
            if (builder.getTokenType() == AsterTokenTypes.COMMA) {
                builder.advanceLexer();
            } else if (builder.getTokenType() != AsterTokenTypes.RPAREN) {
                break;
            }
        }

        // ')'
        if (builder.getTokenType() == AsterTokenTypes.RPAREN) {
            builder.advanceLexer();
        } else {
            builder.error("期望 ')'");
        }

        // 检查是否为 Lambda
        if (builder.getTokenType() == AsterTokenTypes.MINUS) {
            builder.advanceLexer();
            if (builder.getTokenType() == AsterTokenTypes.GT) {
                builder.advanceLexer();
                parseExpression(builder);
                marker.done(AsterElementTypes.LAMBDA_EXPR);
                return;
            }
        }

        marker.drop();
    }

    /**
     * 解析参数列表
     */
    private void parseParameterList(PsiBuilder builder) {
        // '('
        builder.advanceLexer();

        while (!builder.eof() && builder.getTokenType() != AsterTokenTypes.RPAREN) {
            int beforeOffset = builder.getCurrentOffset();
            parseParameter(builder);

            // 防止无限循环：如果没有前进，强制推进
            if (builder.getCurrentOffset() == beforeOffset && !builder.eof()) {
                advanceWithError(builder, "意外的 token");
                continue;
            }

            if (builder.getTokenType() == AsterTokenTypes.COMMA) {
                builder.advanceLexer();
            } else {
                break;
            }
        }

        // ')'
        if (builder.getTokenType() == AsterTokenTypes.RPAREN) {
            builder.advanceLexer();
        } else {
            builder.error("期望 ')'");
        }
    }

    /**
     * 解析单个参数
     */
    private void parseParameter(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 注解（可选）
        while (builder.getTokenType() == AsterTokenTypes.AT) {
            parseAnnotation(builder);
        }

        // 参数名（支持 IDENT 和 TYPE_IDENT）
        boolean hasName = false;
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
            hasName = true;
        } else if (builder.getTokenType() != AsterTokenTypes.RPAREN &&
                   builder.getTokenType() != AsterTokenTypes.COMMA) {
            builder.error("期望参数名");
        }

        // ':' 和类型
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseType(builder);
        } else if (hasName &&
                   builder.getTokenType() != AsterTokenTypes.RPAREN &&
                   builder.getTokenType() != AsterTokenTypes.COMMA) {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.PARAMETER);
    }

    /**
     * 解析类型参数列表
     */
    private void parseTypeParams(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // '['
        builder.advanceLexer();

        while (!builder.eof() && builder.getTokenType() != AsterTokenTypes.RBRACKET) {
            if (builder.getTokenType() == AsterTokenTypes.IDENT ||
                builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                builder.advanceLexer();
            }
            if (builder.getTokenType() == AsterTokenTypes.COMMA) {
                builder.advanceLexer();
            } else if (builder.getTokenType() != AsterTokenTypes.RBRACKET) {
                break;
            }
        }

        // ']'
        if (builder.getTokenType() == AsterTokenTypes.RBRACKET) {
            builder.advanceLexer();
        } else {
            builder.error("期望 ']'");
        }

        marker.done(AsterElementTypes.TYPE_PARAM_LIST);
    }

    /**
     * 解析类型
     */
    private void parseType(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 类型名
        if (builder.getTokenType() == AsterTokenTypes.TYPE_IDENT ||
            builder.getTokenType() == AsterTokenTypes.IDENT) {
            builder.advanceLexer();

            // 泛型参数 [...]
            if (builder.getTokenType() == AsterTokenTypes.LBRACKET) {
                parseTypeArgs(builder);
            }
        } else {
            // 类型名缺失，报告错误
            builder.error("期望类型名");
        }

        marker.done(AsterElementTypes.TYPE_REF);
    }

    /**
     * 解析类型参数
     */
    private void parseTypeArgs(PsiBuilder builder) {
        // '['
        builder.advanceLexer();

        while (!builder.eof() && builder.getTokenType() != AsterTokenTypes.RBRACKET) {
            parseType(builder);
            if (builder.getTokenType() == AsterTokenTypes.COMMA) {
                builder.advanceLexer();
            } else {
                break;
            }
        }

        // ']'
        if (builder.getTokenType() == AsterTokenTypes.RBRACKET) {
            builder.advanceLexer();
        } else {
            builder.error("期望 ']'");
        }
    }

    /**
     * 解析字段列表
     * <p>
     * 由于 Lexer 已过滤 INDENT/DEDENT，使用顶层关键词识别块边界。
     */
    private void parseFieldList(PsiBuilder builder) {
        skipNewlines(builder);

        // 解析字段列表，直到遇到顶层关键词或文件结束
        while (!builder.eof() && !isTopLevelKeyword(builder)) {
            IElementType beforeType = builder.getTokenType();
            int beforeOffset = builder.getCurrentOffset();

            // 跳过注释
            if (beforeType == AsterTokenTypes.COMMENT) {
                builder.advanceLexer();
                skipNewlines(builder);
                continue;
            }

            parseField(builder);
            skipNewlines(builder);

            // 防止无限循环：如果没有前进，强制推进
            if (builder.getCurrentOffset() == beforeOffset && !builder.eof()) {
                advanceWithError(builder, "意外的 token");
            }
        }
    }

    /**
     * 解析单个字段
     */
    private void parseField(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // 注解（可选）
        while (builder.getTokenType() == AsterTokenTypes.AT) {
            parseAnnotation(builder);
        }

        // 字段名（支持 IDENT 和 TYPE_IDENT）
        boolean hasName = false;
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
            hasName = true;
        } else if (builder.getTokenType() != AsterTokenTypes.NEWLINE &&
                   !isTopLevelKeyword(builder)) {
            builder.error("期望字段名");
        }

        // ':' 和类型
        if (builder.getTokenType() == AsterTokenTypes.COLON) {
            builder.advanceLexer();
            parseType(builder);
        } else if (hasName &&
                   builder.getTokenType() != AsterTokenTypes.NEWLINE &&
                   !isTopLevelKeyword(builder)) {
            builder.error("期望 ':'");
        }

        marker.done(AsterElementTypes.FIELD_DEF);
    }

    /**
     * 解析变体列表（枚举）
     * <p>
     * 由于 Lexer 已过滤 INDENT/DEDENT，使用顶层关键词识别块边界。
     */
    private void parseVariantList(PsiBuilder builder) {
        skipNewlines(builder);

        // 解析变体列表，直到遇到顶层关键词或文件结束
        while (!builder.eof() && !isTopLevelKeyword(builder)) {
            int beforeOffset = builder.getCurrentOffset();

            // 跳过注释
            if (builder.getTokenType() == AsterTokenTypes.COMMENT) {
                builder.advanceLexer();
                skipNewlines(builder);
                continue;
            }

            if (builder.getTokenType() == AsterTokenTypes.IDENT ||
                builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                PsiBuilder.Marker variantMarker = builder.mark();
                builder.advanceLexer();
                variantMarker.done(AsterElementTypes.ENUM_VARIANT);
            }
            skipNewlines(builder);

            // 防止无限循环：如果没有前进，强制推进
            if (builder.getCurrentOffset() == beforeOffset && !builder.eof()) {
                advanceWithError(builder, "意外的 token");
            }
        }
    }

    /**
     * 解析注解
     */
    private void parseAnnotation(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();

        // '@'
        builder.advanceLexer();

        // 注解名
        if (builder.getTokenType() == AsterTokenTypes.IDENT ||
            builder.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
            builder.advanceLexer();
        }

        // 参数（可选）
        if (builder.getTokenType() == AsterTokenTypes.LPAREN) {
            builder.advanceLexer();
            while (!builder.eof() && builder.getTokenType() != AsterTokenTypes.RPAREN) {
                builder.advanceLexer();
            }
            if (builder.getTokenType() == AsterTokenTypes.RPAREN) {
                builder.advanceLexer();
            }
        }

        marker.done(AsterElementTypes.ANNOTATION);
    }

    /**
     * 跳过空白和换行
     */
    private void skipWhitespaceAndNewlines(PsiBuilder builder) {
        while (!builder.eof() &&
               (builder.getTokenType() == AsterTokenTypes.NEWLINE ||
                builder.getTokenType() == AsterTokenTypes.WHITE_SPACE)) {
            builder.advanceLexer();
        }
    }

    /**
     * 检查是否是声明关键字
     * <p>
     * 用于导入解析时判断模块路径是否结束，
     * 避免将后续声明（如另一个 import 或 func）误解析为模块路径的一部分。
     */
    private boolean isDeclarationKeyword(String text) {
        if (text == null) return false;
        return switch (text) {
            case "this", "to", "define", "use",  // 自然语言语法
                 "module", "capabilities", "workflow", "func", "data", "enum", "type", "import"  // 传统语法
                 -> true;
            default -> false;
        };
    }

    /**
     * 跳过换行
     */
    private void skipNewlines(PsiBuilder builder) {
        while (!builder.eof() && builder.getTokenType() == AsterTokenTypes.NEWLINE) {
            builder.advanceLexer();
        }
    }

    /**
     * 跳过换行和注释
     * <p>
     * 用于导入解析等场景，支持跨行的 'as' 别名：
     * <pre>
     * Use module.path // comment
     *     as alias.
     * </pre>
     */
    private void skipNewlinesAndComments(PsiBuilder builder) {
        while (!builder.eof()) {
            IElementType type = builder.getTokenType();
            if (type == AsterTokenTypes.NEWLINE || type == AsterTokenTypes.COMMENT) {
                builder.advanceLexer();
            } else {
                break;
            }
        }
    }

    /**
     * 只跳过空白（不跳过换行）
     * 用于 match case body 解析，换行作为 case 边界
     */
    private void skipWhitespace(PsiBuilder builder) {
        while (!builder.eof() && builder.getTokenType() == AsterTokenTypes.WHITE_SPACE) {
            builder.advanceLexer();
        }
    }

    /**
     * 推进并报告错误
     */
    private void advanceWithError(PsiBuilder builder, String message) {
        PsiBuilder.Marker errorMarker = builder.mark();
        builder.advanceLexer();
        errorMarker.error(message);
    }
}
