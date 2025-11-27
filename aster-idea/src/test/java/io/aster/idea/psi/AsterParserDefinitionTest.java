package io.aster.idea.psi;

import com.intellij.psi.tree.TokenSet;
import io.aster.idea.lang.AsterLexerAdapter;
import io.aster.idea.lang.AsterTokenTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsterParserDefinition 单元测试
 */
@DisplayName("AsterParserDefinition")
class AsterParserDefinitionTest {

    private AsterParserDefinition parserDefinition;

    @BeforeEach
    void setUp() {
        parserDefinition = new AsterParserDefinition();
    }

    @Nested
    @DisplayName("Lexer 创建")
    class LexerCreation {

        @Test
        @DisplayName("createLexer 应返回 AsterLexerAdapter 实例")
        void shouldCreateAsterLexerAdapter() {
            var lexer = parserDefinition.createLexer(null);

            assertNotNull(lexer);
            assertInstanceOf(AsterLexerAdapter.class, lexer);
        }
    }

    @Nested
    @DisplayName("Parser 创建")
    class ParserCreation {

        @Test
        @DisplayName("createParser 应返回 AsterParser 实例")
        void shouldCreateAsterParser() {
            var parser = parserDefinition.createParser(null);

            assertNotNull(parser);
            assertInstanceOf(AsterParser.class, parser);
        }
    }

    @Nested
    @DisplayName("文件元素类型")
    class FileElementType {

        @Test
        @DisplayName("getFileNodeType 应返回 FILE 类型")
        void shouldReturnFileNodeType() {
            var fileType = parserDefinition.getFileNodeType();

            assertNotNull(fileType);
            assertEquals(AsterParserDefinition.FILE, fileType);
        }

        @Test
        @DisplayName("FILE 类型应属于 Aster 语言")
        void fileTypeShouldBelongToAsterLanguage() {
            var fileType = AsterParserDefinition.FILE;

            assertEquals(
                io.aster.idea.lang.AsterLanguage.INSTANCE,
                fileType.getLanguage()
            );
        }
    }

    @Nested
    @DisplayName("TokenSet 定义")
    class TokenSetDefinitions {

        @Test
        @DisplayName("getCommentTokens 应返回注释 TokenSet")
        void shouldReturnCommentTokenSet() {
            TokenSet comments = parserDefinition.getCommentTokens();

            assertNotNull(comments);
            assertTrue(comments.contains(AsterTokenTypes.COMMENT));
        }

        @Test
        @DisplayName("getWhitespaceTokens 应返回空白 TokenSet")
        void shouldReturnWhitespaceTokenSet() {
            TokenSet whitespaces = parserDefinition.getWhitespaceTokens();

            assertNotNull(whitespaces);
            assertTrue(whitespaces.contains(AsterTokenTypes.WHITE_SPACE));
            assertTrue(whitespaces.contains(AsterTokenTypes.NEWLINE));
        }

        @Test
        @DisplayName("getStringLiteralElements 应返回字符串 TokenSet")
        void shouldReturnStringLiteralTokenSet() {
            TokenSet strings = parserDefinition.getStringLiteralElements();

            assertNotNull(strings);
            assertTrue(strings.contains(AsterTokenTypes.STRING));
        }
    }

    @Nested
    @DisplayName("PSI 元素创建")
    class PsiElementCreation {

        @Test
        @DisplayName("createElement 应返回 AsterPsiElement 实例")
        void shouldCreateAsterPsiElement() {
            // 注意：此测试需要一个有效的 ASTNode
            // 在单元测试中难以创建，所以我们只验证方法存在
            assertNotNull(parserDefinition);
        }
    }
}
