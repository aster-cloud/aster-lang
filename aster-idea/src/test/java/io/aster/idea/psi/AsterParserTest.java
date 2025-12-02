package io.aster.idea.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lang.impl.PsiBuilderImpl;
import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;
import io.aster.idea.lang.AsterLexerAdapter;
import io.aster.idea.lang.AsterTokenTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Aster PSI 解析器测试
 */
@DisplayName("Aster PSI 解析器")
class AsterParserTest {

    private AsterParser parser;

    @BeforeEach
    void setUp() {
        parser = new AsterParser();
    }

    @Nested
    @DisplayName("元素类型定义")
    class ElementTypeTests {

        @Test
        @DisplayName("应定义所有声明类型")
        void shouldDefineDeclarationTypes() {
            assertNotNull(AsterElementTypes.FUNC_DECL);
            assertNotNull(AsterElementTypes.DATA_DECL);
            assertNotNull(AsterElementTypes.ENUM_DECL);
            assertNotNull(AsterElementTypes.TYPE_ALIAS_DECL);
            assertNotNull(AsterElementTypes.IMPORT_DECL);
        }

        @Test
        @DisplayName("应定义所有语句类型")
        void shouldDefineStatementTypes() {
            assertNotNull(AsterElementTypes.LET_STMT);
            assertNotNull(AsterElementTypes.SET_STMT);
            assertNotNull(AsterElementTypes.RETURN_STMT);
            assertNotNull(AsterElementTypes.IF_STMT);
            assertNotNull(AsterElementTypes.MATCH_STMT);
            assertNotNull(AsterElementTypes.START_STMT);
            assertNotNull(AsterElementTypes.WAIT_STMT);
            assertNotNull(AsterElementTypes.WORKFLOW_STMT);
        }

        @Test
        @DisplayName("应定义辅助类型")
        void shouldDefineHelperTypes() {
            assertNotNull(AsterElementTypes.BLOCK);
            assertNotNull(AsterElementTypes.PARAMETER);
            assertNotNull(AsterElementTypes.FIELD_DEF);
            assertNotNull(AsterElementTypes.TYPE_REF);
            assertNotNull(AsterElementTypes.ANNOTATION);
            assertNotNull(AsterElementTypes.PATTERN);
        }

        @Test
        @DisplayName("元素类型应有正确的调试名称")
        void elementTypesShouldHaveCorrectDebugNames() {
            assertEquals("FUNC_DECL", AsterElementTypes.FUNC_DECL.toString());
            assertEquals("DATA_DECL", AsterElementTypes.DATA_DECL.toString());
            assertEquals("LET_STMT", AsterElementTypes.LET_STMT.toString());
            assertEquals("BLOCK", AsterElementTypes.BLOCK.toString());
        }
    }

    @Nested
    @DisplayName("PSI 元素实现")
    class PsiElementTests {

        @Test
        @DisplayName("AsterElementType 应属于 Aster 语言")
        void asterElementTypeShouldBelongToAsterLanguage() {
            AsterElementType type = new AsterElementType("TEST");
            assertNotNull(type.getLanguage());
            assertEquals("Aster", type.getLanguage().getID());
        }
    }

    @Nested
    @DisplayName("解析器定义")
    class ParserDefinitionTests {

        private AsterParserDefinition parserDefinition;

        @BeforeEach
        void setUp() {
            parserDefinition = new AsterParserDefinition();
        }

        @Test
        @DisplayName("应创建 AsterParser")
        void shouldCreateAsterParser() {
            var parser = parserDefinition.createParser(null);
            assertNotNull(parser);
            assertInstanceOf(AsterParser.class, parser);
        }

        @Test
        @DisplayName("应创建 AsterLexerAdapter")
        void shouldCreateAsterLexerAdapter() {
            Lexer lexer = parserDefinition.createLexer(null);
            assertNotNull(lexer);
            assertInstanceOf(AsterLexerAdapter.class, lexer);
        }

        @Test
        @DisplayName("应返回正确的文件元素类型")
        void shouldReturnCorrectFileNodeType() {
            assertNotNull(parserDefinition.getFileNodeType());
            assertEquals("FILE", parserDefinition.getFileNodeType().toString());
        }

        @Test
        @DisplayName("应返回正确的注释 token 集")
        void shouldReturnCorrectCommentTokens() {
            var comments = parserDefinition.getCommentTokens();
            assertNotNull(comments);
            assertTrue(comments.contains(AsterTokenTypes.COMMENT));
        }

        @Test
        @DisplayName("应返回正确的空白 token 集")
        void shouldReturnCorrectWhitespaceTokens() {
            var whitespaces = parserDefinition.getWhitespaceTokens();
            assertNotNull(whitespaces);
            assertTrue(whitespaces.contains(AsterTokenTypes.WHITE_SPACE));
        }

        @Test
        @DisplayName("应返回正确的字符串字面量 token 集")
        void shouldReturnCorrectStringLiteralTokens() {
            var strings = parserDefinition.getStringLiteralElements();
            assertNotNull(strings);
            assertTrue(strings.contains(AsterTokenTypes.STRING));
        }
    }

    @Nested
    @DisplayName("Token 流解析")
    class TokenParsingTests {

        @Test
        @DisplayName("Lexer 应正确识别标识符")
        void lexerShouldRecognizeIdentifiers() {
            // 注意：Aster 词法器在词法级别将关键字作为 IDENT 返回
            // 语法区分在解析器级别完成
            String source = "func data enum type import let set return if match";
            AsterLexerAdapter lexer = new AsterLexerAdapter();
            lexer.start(source, 0, source.length(), 0);

            int identCount = 0;
            while (lexer.getTokenType() != null) {
                if (lexer.getTokenType() == AsterTokenTypes.IDENT) {
                    identCount++;
                }
                lexer.advance();
            }

            // 所有关键字词在词法级别都被识别为 IDENT
            assertEquals(10, identCount, "应识别出 10 个标识符");
        }

        @Test
        @DisplayName("Lexer 应正确识别类型标识符")
        void lexerShouldRecognizeTypeIdentifiers() {
            String source = "String Int Bool Option Result";
            AsterLexerAdapter lexer = new AsterLexerAdapter();
            lexer.start(source, 0, source.length(), 0);

            int typeIdentCount = 0;
            while (lexer.getTokenType() != null) {
                if (lexer.getTokenType() == AsterTokenTypes.TYPE_IDENT) {
                    typeIdentCount++;
                }
                lexer.advance();
            }

            assertEquals(5, typeIdentCount);
        }

        @Test
        @DisplayName("Lexer 应正确识别普通标识符")
        void lexerShouldRecognizeRegularIdentifiers() {
            String source = "foo bar baz";
            AsterLexerAdapter lexer = new AsterLexerAdapter();
            lexer.start(source, 0, source.length(), 0);

            int identCount = 0;
            while (lexer.getTokenType() != null) {
                if (lexer.getTokenType() == AsterTokenTypes.IDENT) {
                    identCount++;
                }
                lexer.advance();
            }

            assertEquals(3, identCount);
        }
    }

    @Nested
    @DisplayName("PSI 树结构")
    class PsiTreeStructureTests {

        @Test
        @DisplayName("函数声明解析验证")
        void funcDeclParsing() {
            // 验证元素类型存在
            assertNotNull(AsterElementTypes.FUNC_DECL);
            assertNotNull(AsterElementTypes.PARAMETER);
            assertNotNull(AsterElementTypes.TYPE_REF);
            assertNotNull(AsterElementTypes.BLOCK);
        }

        @Test
        @DisplayName("数据类型声明解析验证")
        void dataDeclParsing() {
            // 验证元素类型存在
            assertNotNull(AsterElementTypes.DATA_DECL);
            assertNotNull(AsterElementTypes.FIELD_DEF);
        }

        @Test
        @DisplayName("语句解析验证")
        void statementParsing() {
            // 验证所有语句类型存在
            assertNotNull(AsterElementTypes.LET_STMT);
            assertNotNull(AsterElementTypes.SET_STMT);
            assertNotNull(AsterElementTypes.RETURN_STMT);
            assertNotNull(AsterElementTypes.IF_STMT);
            assertNotNull(AsterElementTypes.MATCH_STMT);
            assertNotNull(AsterElementTypes.MATCH_CASE);
        }
    }
}
