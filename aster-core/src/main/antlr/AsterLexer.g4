/**
 * Aster CNL 词法规则
 *
 * 基于 TypeScript Lexer 迁移而来，包含完整的 token 定义。
 * 注意：缩进处理（INDENT/DEDENT）需要通过自定义 Lexer 类实现。
 */
lexer grammar AsterLexer;

// ============================================================
// 标点符号（Punctuation）
// ============================================================
DOT: '.';
COLON: ':';
COMMA: ',';
LPAREN: '(';
RPAREN: ')';
LBRACKET: '[';
RBRACKET: ']';

// ============================================================
// 运算符（Operators）
// ============================================================
// 多字符运算符必须放在单字符之前，避免优先级问题
LTE: '<=';
GTE: '>=';
NEQ: '!=';

EQUALS: '=';
PLUS: '+';
STAR: '*';
MINUS: '-';
SLASH: '/';
LT: '<';
GT: '>';
QUESTION: '?';
AT: '@';
ARROW: '->';

// ============================================================
// 字面量（Literals）
// ============================================================

// 字符串字面量（支持转义）
STRING_LITERAL: '"' ( ~["\\\r\n] | '\\' . )* '"';

// 布尔字面量
BOOL_LITERAL: 'true' | 'false';

// null 字面量
NULL_LITERAL: 'null';

// 长整型字面量（带 L 后缀）
LONG_LITERAL: [0-9]+ [Ll];

// 浮点数字面量（包含小数点）
FLOAT_LITERAL: [0-9]+ '.' [0-9]+;

// 整数字面量
INT_LITERAL: [0-9]+;

// ============================================================
// 关键字（Keywords）
// ============================================================
// 注意：关键字必须在 IDENT 之前定义，确保优先匹配

// 模块相关
THIS: 'This' | 'this';
MODULE: 'module';
IS: 'is';

// 函数相关
TO: 'To' | 'to';
WITH: 'with';
AND: 'and';
OR: 'or';
PRODUCE: 'produce';

// 类型定义相关
DEFINE: 'Define';
TYPE: 'type';
AS: 'as';
ONE: 'one';
OF: 'of';

// 导入相关
USE: 'Use' | 'use';

// 语句关键字
LET: 'Let';
BE: 'be';
RETURN: 'Return';
IF: 'If';
ELSE: 'Else' | 'Otherwise';
MATCH: 'Match';
WHEN: 'When';
NOT: 'not';
START: 'Start';
WAIT: 'Wait';
FOR: 'for';
ASYNC: 'async';
WORKFLOW: 'Workflow' | 'workflow';
STEP: 'Step' | 'step';
RETRY: 'Retry' | 'retry';
TIMEOUT: 'Timeout' | 'timeout';
DEPENDS: 'Depends' | 'depends';
ON: 'On' | 'on';
COMPENSATE: 'Compensate' | 'compensate';
MAX: 'Max' | 'max';
ATTEMPTS: 'Attempts' | 'attempts';
BACKOFF: 'Backoff' | 'backoff';
SECONDS: 'seconds' | 'second';

// 表达式关键字
FUNCTION: 'function';

// 集合类型关键字
MAP: 'Map';

// 能力标注关键字
IT: 'It';
PERFORMS: 'performs';

// ============================================================
// 标识符（Identifiers）
// ============================================================

// 类型标识符（Uppercase 开头）
TYPE_IDENT: [A-Z][a-zA-Z0-9_]*;

// 普通标识符（lowercase 或 _ 开头）
IDENT: [a-z_][a-zA-Z0-9_]*;

// ============================================================
// 注释（Comments）
// ============================================================

// 单行注释（归入 HIDDEN 通道，后续通过自定义 Lexer 处理 trivia 分类）
COMMENT: '#' ~[\r\n]* -> channel(HIDDEN);

// ============================================================
// 空白符（Whitespace）
// ============================================================

// 换行符（需要单独处理以支持缩进检测）
NEWLINE: '\r'? '\n';

// 行内空白（跳过）
WS: [ \t]+ -> skip;

// ============================================================
// 缩进 tokens（INDENT/DEDENT）
// ============================================================
// 注意：ANTLR4 默认无法生成 INDENT/DEDENT token，
// 需要通过自定义 Lexer 类（AsterCustomLexer）在 nextToken() 中动态生成。
// 参考：Python ANTLR4 grammar 的缩进处理方式。
// ============================================================
