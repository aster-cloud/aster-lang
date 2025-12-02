package io.aster.idea.psi;

import com.intellij.psi.tree.IElementType;
import io.aster.idea.lang.AsterLanguage;

/**
 * Aster PSI 复合元素类型定义
 * <p>
 * 定义 Aster 语言的 PSI 节点类型（非 Token 类型）。
 * 这些类型用于构建语法树结构。
 */
public interface AsterElementTypes {

    // ============ 声明 (Declarations) ============

    /** 模块声明 */
    IElementType MODULE_DECL = new AsterElementType("MODULE_DECL");

    /** Capabilities 声明 */
    IElementType CAPABILITIES_DECL = new AsterElementType("CAPABILITIES_DECL");

    /** Uses 子句 */
    IElementType USES_CLAUSE = new AsterElementType("USES_CLAUSE");

    /** 导入声明 */
    IElementType IMPORT_DECL = new AsterElementType("IMPORT_DECL");

    /** 数据类型声明 */
    IElementType DATA_DECL = new AsterElementType("DATA_DECL");

    /** 枚举声明 */
    IElementType ENUM_DECL = new AsterElementType("ENUM_DECL");

    /** 类型别名声明 */
    IElementType TYPE_ALIAS_DECL = new AsterElementType("TYPE_ALIAS_DECL");

    /** 函数声明 */
    IElementType FUNC_DECL = new AsterElementType("FUNC_DECL");

    /** 参数定义 */
    IElementType PARAMETER = new AsterElementType("PARAMETER");

    /** 字段定义 */
    IElementType FIELD_DEF = new AsterElementType("FIELD_DEF");

    /** 枚举变体 */
    IElementType ENUM_VARIANT = new AsterElementType("ENUM_VARIANT");

    // ============ 语句 (Statements) ============

    /** Let 绑定语句 */
    IElementType LET_STMT = new AsterElementType("LET_STMT");

    /** Set 赋值语句 */
    IElementType SET_STMT = new AsterElementType("SET_STMT");

    /** Return 语句 */
    IElementType RETURN_STMT = new AsterElementType("RETURN_STMT");

    /** If 语句 */
    IElementType IF_STMT = new AsterElementType("IF_STMT");

    /** Match 语句 */
    IElementType MATCH_STMT = new AsterElementType("MATCH_STMT");

    /** Match 分支 */
    IElementType MATCH_CASE = new AsterElementType("MATCH_CASE");

    /** Start 语句 */
    IElementType START_STMT = new AsterElementType("START_STMT");

    /** Wait 语句 */
    IElementType WAIT_STMT = new AsterElementType("WAIT_STMT");

    /** For 循环语句 */
    IElementType FOR_STMT = new AsterElementType("FOR_STMT");

    /** While 循环语句 */
    IElementType WHILE_STMT = new AsterElementType("WHILE_STMT");

    /** It performs 语句（自然语言工作流）*/
    IElementType IT_PERFORMS_STMT = new AsterElementType("IT_PERFORMS_STMT");

    /** Workflow 语句 */
    IElementType WORKFLOW_STMT = new AsterElementType("WORKFLOW_STMT");

    /** Workflow Step */
    IElementType WORKFLOW_STEP = new AsterElementType("WORKFLOW_STEP");

    /** Workflow Compensate */
    IElementType WORKFLOW_COMPENSATE = new AsterElementType("WORKFLOW_COMPENSATE");

    /** Workflow Timeout */
    IElementType WORKFLOW_TIMEOUT = new AsterElementType("WORKFLOW_TIMEOUT");

    /** Workflow Retry */
    IElementType WORKFLOW_RETRY = new AsterElementType("WORKFLOW_RETRY");

    /** 代码块 */
    IElementType BLOCK = new AsterElementType("BLOCK");

    // ============ 表达式 (Expressions) ============

    /** 名称表达式 */
    IElementType NAME_EXPR = new AsterElementType("NAME_EXPR");

    /** 字面量表达式 */
    IElementType LITERAL_EXPR = new AsterElementType("LITERAL_EXPR");

    /** 函数调用表达式 */
    IElementType CALL_EXPR = new AsterElementType("CALL_EXPR");

    /** 构造表达式 */
    IElementType CONSTRUCT_EXPR = new AsterElementType("CONSTRUCT_EXPR");

    /** 构造字段 */
    IElementType CONSTRUCT_FIELD = new AsterElementType("CONSTRUCT_FIELD");

    /** Result Ok 表达式 */
    IElementType OK_EXPR = new AsterElementType("OK_EXPR");

    /** Result Err 表达式 */
    IElementType ERR_EXPR = new AsterElementType("ERR_EXPR");

    /** Option Some 表达式 */
    IElementType SOME_EXPR = new AsterElementType("SOME_EXPR");

    /** Option None 表达式 */
    IElementType NONE_EXPR = new AsterElementType("NONE_EXPR");

    /** 列表字面量表达式 */
    IElementType LIST_LITERAL_EXPR = new AsterElementType("LIST_LITERAL_EXPR");

    /** Lambda 表达式 */
    IElementType LAMBDA_EXPR = new AsterElementType("LAMBDA_EXPR");

    /** Await 表达式 */
    IElementType AWAIT_EXPR = new AsterElementType("AWAIT_EXPR");

    /** 二元表达式 */
    IElementType BINARY_EXPR = new AsterElementType("BINARY_EXPR");

    /** 成员访问表达式 */
    IElementType MEMBER_EXPR = new AsterElementType("MEMBER_EXPR");

    /** 参数列表 */
    IElementType ARGUMENT_LIST = new AsterElementType("ARGUMENT_LIST");

    // ============ 类型 (Types) ============

    /** 类型引用 */
    IElementType TYPE_REF = new AsterElementType("TYPE_REF");

    /** 类型参数列表 */
    IElementType TYPE_PARAM_LIST = new AsterElementType("TYPE_PARAM_LIST");

    // ============ 模式 (Patterns) ============

    /** 模式 */
    IElementType PATTERN = new AsterElementType("PATTERN");

    // ============ 注解 (Annotations) ============

    /** 注解 */
    IElementType ANNOTATION = new AsterElementType("ANNOTATION");

    // ============ 其他 ============

    /** 效应列表 */
    IElementType EFFECT_LIST = new AsterElementType("EFFECT_LIST");
}
