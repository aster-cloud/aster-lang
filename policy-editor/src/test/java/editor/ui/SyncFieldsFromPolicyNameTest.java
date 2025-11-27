package editor.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import editor.util.PolicyNameParser;
import editor.util.PolicyNameParser.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * PolicyNameParser 解析逻辑单元测试。
 * 测试策略名称到 moduleField/functionField 的解析规则。
 *
 * 注意：直接测试 PolicyNameParser 工具类，确保生产代码和测试使用同一逻辑。
 */
class SyncFieldsFromPolicyNameTest {

    @ParameterizedTest
    @DisplayName("标准策略名称解析")
    @CsvSource({
        "aster.finance.loan, aster.finance, loan",
        "test.module.evaluate, test.module, evaluate",
        "simple.func, simple, func",
        "a.b.c.d.e, a.b.c.d, e"
    })
    void testStandardPolicyNameParsing(String input, String expectedModule, String expectedFunc) {
        ParseResult result = PolicyNameParser.parse(input);
        assertEquals(expectedModule, result.getModuleName(), "模块部分解析错误");
        assertEquals(expectedFunc, result.getFunctionName(), "函数部分解析错误");
    }

    @Test
    @DisplayName("无点号策略名称 - 函数默认为 evaluate")
    void testNoDotPolicyName() {
        ParseResult result = PolicyNameParser.parse("standalone");
        assertEquals("standalone", result.getModuleName());
        assertEquals("evaluate", result.getFunctionName());
    }

    @ParameterizedTest
    @DisplayName("空值和空白输入 - 返回 null 表示不修改")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testNullAndEmptyInput(String input) {
        ParseResult result = PolicyNameParser.parse(input);
        assertNull(result, "空值/空白应返回 null");
    }

    @Test
    @DisplayName("边界情况：策略名以点结尾")
    void testTrailingDot() {
        // "module." -> modulePart="module", funcPart="" -> funcPart="evaluate"
        ParseResult result = PolicyNameParser.parse("module.");
        assertEquals("module", result.getModuleName(), "模块部分应为 module");
        assertEquals("evaluate", result.getFunctionName(), "空函数部分应回退为 evaluate");
    }

    @Test
    @DisplayName("边界情况：策略名以点开头")
    void testLeadingDot() {
        // ".function" -> modulePart="", funcPart="function" -> modulePart="default"
        ParseResult result = PolicyNameParser.parse(".function");
        assertEquals("default", result.getModuleName(), "空模块部分应回退为 default");
        assertEquals("function", result.getFunctionName(), "函数部分应为 function");
    }

    @Test
    @DisplayName("边界情况：只有一个点")
    void testSingleDot() {
        // "." -> modulePart="", funcPart="" -> "default", "evaluate"
        ParseResult result = PolicyNameParser.parse(".");
        assertEquals("default", result.getModuleName(), "空模块应回退为 default");
        assertEquals("evaluate", result.getFunctionName(), "空函数应回退为 evaluate");
    }

    @Test
    @DisplayName("带空白的策略名称")
    void testWhitespaceInName() {
        ParseResult result = PolicyNameParser.parse("  aster.finance.loan  ");
        assertEquals("aster.finance", result.getModuleName());
        assertEquals("loan", result.getFunctionName());
    }

    @Test
    @DisplayName("多层模块名称正确解析")
    void testDeepNestedModule() {
        ParseResult result = PolicyNameParser.parse("com.example.app.service.user.validate");
        assertEquals("com.example.app.service.user", result.getModuleName());
        assertEquals("validate", result.getFunctionName());
    }

    @Test
    @DisplayName("连续点号处理")
    void testConsecutiveDots() {
        // "a..b" -> lastDot 在第 2 个点，modulePart="a.", funcPart="b"
        ParseResult result = PolicyNameParser.parse("a..b");
        assertEquals("a.", result.getModuleName(), "modulePart 应为 a.");
        assertEquals("b", result.getFunctionName(), "funcPart 应为 b");
    }
}
