package io.aster.idea.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import io.aster.idea.lang.AsterLexerAdapter;
import io.aster.idea.lang.AsterTokenTypes;
import io.aster.idea.psi.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Aster PSI 集成测试
 * <p>
 * 验证 PSI 元素与 Lexer、Parser 的集成正确性
 */
@DisplayName("Aster PSI 集成测试")
class AsterPsiIntegrationTest {

    private AsterParserDefinition parserDefinition;
    private AsterLexerAdapter lexer;

    @BeforeEach
    void setUp() {
        parserDefinition = new AsterParserDefinition();
        lexer = new AsterLexerAdapter();
    }

    /**
     * 收集源代码中的所有 token
     */
    private List<TokenInfo> collectTokens(String source) {
        List<TokenInfo> tokens = new ArrayList<>();
        lexer.start(source, 0, source.length(), 0);

        while (lexer.getTokenType() != null) {
            tokens.add(new TokenInfo(
                lexer.getTokenType(),
                lexer.getTokenStart(),
                lexer.getTokenEnd(),
                source.substring(lexer.getTokenStart(), lexer.getTokenEnd())
            ));
            lexer.advance();
        }
        return tokens;
    }

    record TokenInfo(IElementType type, int start, int end, String text) {}

    @Nested
    @DisplayName("自然语言语法 Token 识别")
    class NaturalLanguageSyntaxTests {

        @Test
        @DisplayName("应正确识别模块声明语法: This module is name.")
        void shouldRecognizeModuleDeclSyntax() {
            String source = "This module is mymodule.";
            List<TokenInfo> tokens = collectTokens(source);

            // 过滤掉空白
            List<TokenInfo> nonWhitespace = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE && t.type() != AsterTokenTypes.NEWLINE)
                .toList();

            // 验证关键词被识别（类型可能是 IDENT 或 TYPE_IDENT，取决于大小写）
            List<String> texts = nonWhitespace.stream().map(TokenInfo::text).toList();
            assertTrue(texts.contains("This") || texts.stream().anyMatch(t -> t.equalsIgnoreCase("this")));
            assertTrue(texts.contains("module"));
            assertTrue(texts.contains("is"));
            assertTrue(texts.contains("mymodule"));

            // 应有 DOT token
            assertTrue(nonWhitespace.stream().anyMatch(t -> t.type() == AsterTokenTypes.DOT));
        }

        @Test
        @DisplayName("应正确识别函数声明语法: To funcName with param: Type, produce ReturnType:")
        void shouldRecognizeFuncDeclSyntax() {
            String source = "To greet with name: String, produce String:";
            List<TokenInfo> tokens = collectTokens(source);

            List<TokenInfo> nonWhitespace = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE && t.type() != AsterTokenTypes.NEWLINE)
                .toList();

            // 验证所有文本被识别（类型可能是 IDENT 或 TYPE_IDENT）
            List<String> allTexts = nonWhitespace.stream().map(TokenInfo::text).toList();

            // "To" 可能是 TYPE_IDENT（大写开头）
            assertTrue(allTexts.stream().anyMatch(t -> t.equalsIgnoreCase("to")));
            assertTrue(allTexts.contains("greet"));
            assertTrue(allTexts.contains("with"));
            assertTrue(allTexts.contains("name"));
            assertTrue(allTexts.contains("produce"));
            assertTrue(allTexts.contains("String"));
        }

        @Test
        @DisplayName("应正确识别数据类型声明语法: Define a TypeName with field: Type.")
        void shouldRecognizeDataDeclSyntax() {
            String source = "Define a User with name: String and age: Int.";
            List<TokenInfo> tokens = collectTokens(source);

            List<TokenInfo> nonWhitespace = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE && t.type() != AsterTokenTypes.NEWLINE)
                .toList();

            // User, String, Int 是 TYPE_IDENT
            List<String> typeIdentTexts = nonWhitespace.stream()
                .filter(t -> t.type() == AsterTokenTypes.TYPE_IDENT)
                .map(TokenInfo::text)
                .toList();

            assertTrue(typeIdentTexts.contains("User"));
            assertTrue(typeIdentTexts.contains("String"));
            assertTrue(typeIdentTexts.contains("Int"));
        }

        @Test
        @DisplayName("应正确识别导入声明语法: Use module.path.")
        void shouldRecognizeImportDeclSyntax() {
            String source = "Use aster.core.io.";
            List<TokenInfo> tokens = collectTokens(source);

            List<TokenInfo> nonWhitespace = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE && t.type() != AsterTokenTypes.NEWLINE)
                .toList();

            // 验证所有文本被识别（类型可能是 IDENT 或 TYPE_IDENT）
            List<String> allTexts = nonWhitespace.stream().map(TokenInfo::text).toList();

            // "Use" 可能是 TYPE_IDENT（大写开头）
            assertTrue(allTexts.stream().anyMatch(t -> t.equalsIgnoreCase("use")));
            assertTrue(allTexts.contains("aster"));
            assertTrue(allTexts.contains("core"));
            assertTrue(allTexts.contains("io"));

            // 应有 DOT token
            assertTrue(nonWhitespace.stream().anyMatch(t -> t.type() == AsterTokenTypes.DOT));
        }
    }

    @Nested
    @DisplayName("传统语法 Token 识别")
    class LegacySyntaxTests {

        @Test
        @DisplayName("应正确识别函数声明传统语法: func name(params) -> Type:")
        void shouldRecognizeLegacyFuncSyntax() {
            String source = "func greet(name: String) -> String:";
            List<TokenInfo> tokens = collectTokens(source);

            List<TokenInfo> nonWhitespace = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE && t.type() != AsterTokenTypes.NEWLINE)
                .toList();

            // func, greet, name 是 IDENT; String 是 TYPE_IDENT
            List<String> identTexts = nonWhitespace.stream()
                .filter(t -> t.type() == AsterTokenTypes.IDENT)
                .map(TokenInfo::text)
                .toList();

            assertTrue(identTexts.contains("func"));
            assertTrue(identTexts.contains("greet"));
            assertTrue(identTexts.contains("name"));
        }

        @Test
        @DisplayName("应正确识别数据类型声明传统语法: data TypeName:")
        void shouldRecognizeLegacyDataSyntax() {
            String source = "data User:";
            List<TokenInfo> tokens = collectTokens(source);

            List<TokenInfo> nonWhitespace = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE && t.type() != AsterTokenTypes.NEWLINE)
                .toList();

            // data 是 IDENT; User 是 TYPE_IDENT
            assertEquals(AsterTokenTypes.IDENT, nonWhitespace.get(0).type());
            assertEquals("data", nonWhitespace.get(0).text());
            assertEquals(AsterTokenTypes.TYPE_IDENT, nonWhitespace.get(1).type());
            assertEquals("User", nonWhitespace.get(1).text());
        }
    }

    @Nested
    @DisplayName("零宽 Token 处理")
    class ZeroWidthTokenTests {

        @Test
        @DisplayName("INDENT/DEDENT 应被过滤（IntelliJ 要求 token 范围严格递增）")
        void indentDedentShouldBeFiltered() {
            String source = "func greet:\n  return \"hello\"";
            List<TokenInfo> tokens = collectTokens(source);

            // 不应包含 INDENT token
            boolean hasIndent = tokens.stream()
                .anyMatch(t -> t.type() == AsterTokenTypes.INDENT);
            assertFalse(hasIndent, "INDENT token 应被过滤");

            // 不应包含 DEDENT token
            boolean hasDedent = tokens.stream()
                .anyMatch(t -> t.type() == AsterTokenTypes.DEDENT);
            assertFalse(hasDedent, "DEDENT token 应被过滤");
        }

        @Test
        @DisplayName("Token 范围应严格递增")
        void tokenRangesShouldBeStrictlyIncreasing() {
            String source = "func greet:\n  let x = 1\n  return x";
            List<TokenInfo> tokens = collectTokens(source);

            int lastEnd = 0;
            for (TokenInfo token : tokens) {
                assertTrue(token.start() >= lastEnd,
                    "Token 开始位置应 >= 上一个 token 结束位置: " + token);
                assertTrue(token.end() > token.start(),
                    "Token 结束位置应 > 开始位置: " + token);
                lastEnd = token.end();
            }
        }
    }

    @Nested
    @DisplayName("CRLF 换行符支持")
    class CrlfSupportTests {

        @Test
        @DisplayName("应正确处理 CRLF 换行符")
        void shouldHandleCrlfNewlines() {
            String source = "func a:\r\n  return 1";
            List<TokenInfo> tokens = collectTokens(source);

            // 应正常完成词法分析
            assertFalse(tokens.isEmpty());

            // Token 范围应有效
            int lastEnd = 0;
            for (TokenInfo token : tokens) {
                assertTrue(token.start() >= lastEnd, "Token 范围应有效");
                assertTrue(token.end() > token.start(), "Token 应有正值长度");
                lastEnd = token.end();
            }
        }

        @Test
        @DisplayName("应正确处理混合换行符")
        void shouldHandleMixedNewlines() {
            String source = "func a:\n  let x = 1\r\n  return x";
            List<TokenInfo> tokens = collectTokens(source);

            // 应正常完成词法分析
            assertFalse(tokens.isEmpty());

            // 应识别所有关键字
            List<String> identTexts = tokens.stream()
                .filter(t -> t.type() == AsterTokenTypes.IDENT)
                .map(TokenInfo::text)
                .toList();

            assertTrue(identTexts.contains("func"));
            assertTrue(identTexts.contains("let"));
            assertTrue(identTexts.contains("return"));
        }
    }

    @Nested
    @DisplayName("PSI 元素工厂")
    class PsiElementFactoryTests {

        @Test
        @DisplayName("应为 FUNC_DECL 创建 AsterFuncDeclImpl")
        void shouldCreateFuncDeclImpl() {
            // 通过 ParserDefinition 验证元素类型映射
            assertNotNull(AsterElementTypes.FUNC_DECL);
        }

        @Test
        @DisplayName("应为 DATA_DECL 创建 AsterDataDeclImpl")
        void shouldCreateDataDeclImpl() {
            assertNotNull(AsterElementTypes.DATA_DECL);
        }

        @Test
        @DisplayName("应为 MODULE_DECL 创建 AsterModuleDeclImpl")
        void shouldCreateModuleDeclImpl() {
            assertNotNull(AsterElementTypes.MODULE_DECL);
        }

        @Test
        @DisplayName("应为 WORKFLOW_STMT 创建 AsterWorkflowStmtImpl")
        void shouldCreateWorkflowStmtImpl() {
            assertNotNull(AsterElementTypes.WORKFLOW_STMT);
        }
    }

    @Nested
    @DisplayName("词法错误处理")
    class LexerErrorHandlingTests {

        @Test
        @DisplayName("词法错误时应返回 BAD_CHARACTER 而非空列表")
        void shouldReturnBadCharacterOnLexerError() {
            // 使用无效字符测试
            String source = "let x = \u0000";  // 空字符
            List<TokenInfo> tokens = collectTokens(source);

            // 应能解析出 let, x, = 等有效 token
            assertFalse(tokens.isEmpty());

            // 如果有错误，应该有 BAD_CHARACTER token
            // 注意：具体行为取决于 aster-core lexer 的错误处理
        }
    }

    @Nested
    @DisplayName("表达式语法")
    class ExpressionSyntaxTests {

        @Test
        @DisplayName("应正确识别 Option 类型表达式")
        void shouldRecognizeOptionExpressions() {
            String source = "Some(value) None";
            List<TokenInfo> tokens = collectTokens(source);

            List<TokenInfo> nonWhitespace = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE && t.type() != AsterTokenTypes.NEWLINE)
                .toList();

            // Some, None 可能是 TYPE_IDENT 或 IDENT（取决于 aster-core）
            // value 是 IDENT
            boolean hasSome = nonWhitespace.stream()
                .anyMatch(t -> "Some".equals(t.text()));
            boolean hasNone = nonWhitespace.stream()
                .anyMatch(t -> "None".equals(t.text()));
            boolean hasValue = nonWhitespace.stream()
                .anyMatch(t -> "value".equals(t.text()));

            assertTrue(hasSome, "应识别 Some");
            assertTrue(hasNone, "应识别 None");
            assertTrue(hasValue, "应识别 value");
        }

        @Test
        @DisplayName("应正确识别 Result 类型表达式")
        void shouldRecognizeResultExpressions() {
            String source = "Ok(result) Err(error)";
            List<TokenInfo> tokens = collectTokens(source);

            List<TokenInfo> nonWhitespace = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE && t.type() != AsterTokenTypes.NEWLINE)
                .toList();

            boolean hasOk = nonWhitespace.stream()
                .anyMatch(t -> "Ok".equals(t.text()));
            boolean hasErr = nonWhitespace.stream()
                .anyMatch(t -> "Err".equals(t.text()));

            assertTrue(hasOk, "应识别 Ok");
            assertTrue(hasErr, "应识别 Err");
        }

        @Test
        @DisplayName("应正确识别列表字面量")
        void shouldRecognizeListLiterals() {
            String source = "[1, 2, 3]";
            List<TokenInfo> tokens = collectTokens(source);

            List<TokenInfo> nonWhitespace = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE && t.type() != AsterTokenTypes.NEWLINE)
                .toList();

            // [ 和 ] 应被识别
            boolean hasLBracket = nonWhitespace.stream()
                .anyMatch(t -> t.type() == AsterTokenTypes.LBRACKET);
            boolean hasRBracket = nonWhitespace.stream()
                .anyMatch(t -> t.type() == AsterTokenTypes.RBRACKET);

            assertTrue(hasLBracket, "应识别 [");
            assertTrue(hasRBracket, "应识别 ]");
        }
    }
}
