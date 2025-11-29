package io.aster.idea.editor;

import com.intellij.lang.BracePair;
import io.aster.idea.lang.AsterTokenTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Aster 编辑器组件单元测试
 */
@DisplayName("Aster 编辑器组件")
class AsterEditorComponentsTest {

    @Nested
    @DisplayName("AsterCommenter")
    class CommenterTests {

        private final AsterCommenter commenter = new AsterCommenter();

        @Test
        @DisplayName("行注释前缀应为 # ")
        void shouldReturnLineCommentPrefix() {
            assertEquals("# ", commenter.getLineCommentPrefix());
        }

        @Test
        @DisplayName("不支持块注释前缀")
        void shouldNotSupportBlockCommentPrefix() {
            assertNull(commenter.getBlockCommentPrefix());
        }

        @Test
        @DisplayName("不支持块注释后缀")
        void shouldNotSupportBlockCommentSuffix() {
            assertNull(commenter.getBlockCommentSuffix());
        }

        @Test
        @DisplayName("不支持嵌套块注释前缀")
        void shouldNotSupportCommentedBlockCommentPrefix() {
            assertNull(commenter.getCommentedBlockCommentPrefix());
        }

        @Test
        @DisplayName("不支持嵌套块注释后缀")
        void shouldNotSupportCommentedBlockCommentSuffix() {
            assertNull(commenter.getCommentedBlockCommentSuffix());
        }
    }

    @Nested
    @DisplayName("AsterBraceMatcher")
    class BraceMatcherTests {

        private final AsterBraceMatcher braceMatcher = new AsterBraceMatcher();

        @Test
        @DisplayName("应定义两对括号")
        void shouldDefineTwoBracePairs() {
            BracePair[] pairs = braceMatcher.getPairs();

            assertEquals(2, pairs.length);
        }

        @Test
        @DisplayName("应包含圆括号配对")
        void shouldContainParenthesesPair() {
            BracePair[] pairs = braceMatcher.getPairs();

            boolean hasParenPair = false;
            for (BracePair pair : pairs) {
                if (pair.getLeftBraceType() == AsterTokenTypes.LPAREN &&
                    pair.getRightBraceType() == AsterTokenTypes.RPAREN) {
                    hasParenPair = true;
                    break;
                }
            }

            assertTrue(hasParenPair, "应包含圆括号配对");
        }

        @Test
        @DisplayName("应包含方括号配对")
        void shouldContainBracketPair() {
            BracePair[] pairs = braceMatcher.getPairs();

            boolean hasBracketPair = false;
            for (BracePair pair : pairs) {
                if (pair.getLeftBraceType() == AsterTokenTypes.LBRACKET &&
                    pair.getRightBraceType() == AsterTokenTypes.RBRACKET) {
                    hasBracketPair = true;
                    break;
                }
            }

            assertTrue(hasBracketPair, "应包含方括号配对");
        }

        @Test
        @DisplayName("括号配对应非结构性")
        void bracePairsShouldNotBeStructural() {
            BracePair[] pairs = braceMatcher.getPairs();

            for (BracePair pair : pairs) {
                assertFalse(pair.isStructural(),
                    "括号配对 " + pair.getLeftBraceType() + " 不应是结构性的");
            }
        }

        @Test
        @DisplayName("isPairedBracesAllowedBeforeType 应返回 true")
        void shouldAllowPairedBracesBeforeAnyType() {
            assertTrue(braceMatcher.isPairedBracesAllowedBeforeType(
                AsterTokenTypes.LPAREN, AsterTokenTypes.IDENT));
            assertTrue(braceMatcher.isPairedBracesAllowedBeforeType(
                AsterTokenTypes.LBRACKET, null));
        }

        @Test
        @DisplayName("getCodeConstructStart 应返回开括号偏移量")
        void shouldReturnOpeningBraceOffset() {
            int offset = 10;
            assertEquals(offset, braceMatcher.getCodeConstructStart(null, offset));
        }
    }
}
