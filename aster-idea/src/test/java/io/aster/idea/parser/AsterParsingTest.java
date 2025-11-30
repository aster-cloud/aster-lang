package io.aster.idea.parser;

import com.intellij.testFramework.ParsingTestCase;
import io.aster.idea.psi.AsterParserDefinition;

/**
 * Aster 解析器测试
 * <p>
 * 使用 IntelliJ ParsingTestCase 测试解析器的 PSI 树生成。
 * 测试文件位于 src/test/testData/parser/ 目录。
 * <p>
 * 正向用例使用 doTest(true, true) 确保无 PsiErrorElement；
 * 错误恢复用例使用 doTest(true) 允许包含 PsiErrorElement。
 */
public class AsterParsingTest extends ParsingTestCase {

    public AsterParsingTest() {
        super("parser", "aster", new AsterParserDefinition());
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    @Override
    protected boolean skipSpaces() {
        return true;
    }

    @Override
    protected boolean includeRanges() {
        return true;
    }

    // ==================== 基础语法测试 ====================

    /**
     * 测试简单函数解析（传统语法 func）
     */
    public void testSimpleFunction() {
        doTest(true, true);
    }

    /**
     * 测试模块声明解析（传统语法 module）
     */
    public void testModuleDeclaration() {
        doTest(true, true);
    }

    // ==================== CNL 语法测试 ====================

    /**
     * 测试数据类型定义（Define ... with）
     */
    public void testDataDefinition() {
        doTest(true, true);
    }

    /**
     * 测试 Match 表达式
     */
    public void testMatchExpression() {
        doTest(true, true);
    }

    /**
     * 测试 If/Otherwise 条件表达式
     */
    public void testIfExpression() {
        doTest(true, true);
    }

    /**
     * 测试 Let 绑定
     */
    public void testLetBinding() {
        doTest(true, true);
    }

    /**
     * 测试 Workflow 工作流定义
     */
    public void testWorkflow() {
        doTest(true, true);
    }

    /**
     * 测试 Use 导入
     */
    public void testUseImport() {
        doTest(true, true);
    }

    // ==================== 传统语法补充测试 ====================

    /**
     * 测试 capabilities 声明
     */
    public void testCapabilities() {
        doTest(true, true);
    }

    /**
     * 测试 enum 枚举声明
     */
    public void testEnumDeclaration() {
        doTest(true, true);
    }

    /**
     * 测试 type 类型别名声明
     */
    public void testTypeAlias() {
        doTest(true, true);
    }

    /**
     * 测试 data 数据类型声明（传统语法）
     */
    public void testDataLegacy() {
        doTest(true, true);
    }

    // ==================== 错误恢复测试 ====================
    // 注意：错误测试使用 doTest(true) 生成黄金文件，允许包含 PsiErrorElement

    /**
     * 测试未闭合括号的错误恢复
     * 预期：解析器应能识别未闭合的括号并生成相应的 PSI 结构
     */
    public void testErrorUnclosedParen() {
        doTest(true);
    }

    /**
     * 测试缺少冒号的错误恢复
     * 预期：解析器应能处理缺少冒号的情况
     */
    public void testErrorMissingColon() {
        doTest(true);
    }

    /**
     * 测试无效 token 的错误恢复
     * 预期：解析器应能处理无效 token 并继续解析
     */
    public void testErrorInvalidToken() {
        doTest(true);
    }
}
