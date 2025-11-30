/**
 * @module lexer
 *
 * 词法分析器：将规范化的 CNL 源代码转换为 Token 流。
 *
 * **功能**：
 * - 识别关键字、标识符、字面量、运算符和标点符号
 * - 处理缩进敏感的语法（INDENT/DEDENT token）
 * - 支持字符串插值和多行字符串
 * - 跟踪每个 token 的位置信息（行号和列号）
 *
 * **缩进规则**：
 * - Aster 使用 2 空格缩进
 * - 缩进必须是偶数个空格
 * - 缩进变化会生成 INDENT 或 DEDENT token
 */

import { TokenKind, KW } from './tokens.js';
import type { Token, Position } from './types.js';
import { Diagnostics } from './diagnostics.js';

function isLetter(ch: string): boolean {
  return /[A-Za-z]/.test(ch);
}

function isDigit(ch: string): boolean {
  return /[0-9]/.test(ch);
}

function isLineBreak(ch: string): boolean {
  return ch === '\n' || ch === '\r';
}

/**
 * 对规范化的 CNL 源代码进行词法分析，生成 Token 流。
 *
 * 这是 Aster 编译管道的第二步，将规范化的文本字符串转换为结构化的 token 序列，
 * 为后续的语法分析阶段提供输入。
 *
 * **Token 类型**：
 * - 关键字：`To`, `Return`, `Match`, `When`, `Define`, `It performs` 等
 * - 标识符：变量名、函数名、类型名
 * - 字面量：整数、浮点数、布尔值、字符串、null
 * - 运算符：`+`, `-`, `*`, `/`, `=`, `==`, `<`, `>` 等
 * - 标点符号：`.`, `,`, `:`, `(`, `)`, `{`, `}` 等
 * - 特殊 token：`INDENT`, `DEDENT`, `NEWLINE`, `EOF`
 *
 * @param input - 规范化后的 CNL 源代码（应先通过 canonicalizer.canonicalize 处理）
 * @returns Token 数组，每个 token 包含类型、值和位置信息
 *
 * @throws {DiagnosticError} 当遇到非法字符或缩进错误时抛出
 *
 * @example
 * ```typescript
 * import { canonicalize, lex } from '@wontlost-ltd/aster-lang';
 *
 * const src = `This module is app.
 * To greet, produce Text:
 *   Return "Hello".
 * `;
 *
 * const canonical = canonicalize(src);
 * const tokens = lex(canonical);
 *
 * // tokens 包含：MODULE_IS, IDENT("app"), DOT, TO, IDENT("greet"), ...
 * console.log(tokens.map(t => t.kind));
 * ```
 */
export function lex(input: string): Token[] {
  const tokens: Token[] = [];
  let i = 0;
  let line = 1;
  let col = 1;

  const push = (
    kind: TokenKind,
    value: Token['value'] = null,
    start: Position = { line, col },
    channel?: Token['channel']
  ): void => {
    const tokenBase = { kind, value, start, end: { line, col } } as const;
    tokens.push(channel ? { ...tokenBase, channel } : tokenBase);
  };

  const peek = (): string => input[i] || '';
  // 跟踪上一个字符是否为 \r，用于正确处理 CRLF 避免重复计数
  let lastWasCR = false;
  const next = (): string => {
    const ch = input[i++] || '';
    if (ch === '\n') {
      // 如果前一个字符是 \r（CRLF 情况），不重复递增行号
      if (!lastWasCR) {
        line++;
      }
      col = 1;
      lastWasCR = false;
    } else if (ch === '\r') {
      // CR 视为换行符，递增行号
      line++;
      col = 1;
      lastWasCR = true;
    } else {
      col++;
      lastWasCR = false;
    }
    return ch;
  };

  const INDENT_STACK = [0];

  const findPrevSignificantToken = (): Token | undefined => {
    for (let idx = tokens.length - 1; idx >= 0; idx--) {
      const token = tokens[idx]!;
      if (token.channel === 'trivia') continue;
      if (
        token.kind === TokenKind.NEWLINE ||
        token.kind === TokenKind.INDENT ||
        token.kind === TokenKind.DEDENT
      ) {
        continue;
      }
      return token;
    }
    return undefined;
  };

  const emitCommentToken = (prefixLength: 1 | 2): void => {
    const startPos = { line, col };
    let raw = '';
    for (let j = 0; j < prefixLength; j++) {
      raw += next();
    }
    while (i < input.length && !isLineBreak(peek())) {
      raw += next();
    }
    const body = raw.slice(prefixLength).replace(/^\s*/, '');
    const prev = findPrevSignificantToken();
    const trivia: 'inline' | 'standalone' =
      prev && prev.end.line === startPos.line ? 'inline' : 'standalone';
    push(
      TokenKind.COMMENT,
      { raw, text: body, trivia },
      startPos,
      'trivia'
    );
  };

  function emitIndentDedent(spaces: number): void {
    const last = INDENT_STACK[INDENT_STACK.length - 1]!;
    if (spaces === last) return;
    if (spaces % 2 !== 0) {
      Diagnostics.invalidIndentation({ line, col }).throw();
    }
    if (spaces > last) {
      INDENT_STACK.push(spaces);
      push(TokenKind.INDENT);
    } else {
      while (INDENT_STACK.length && spaces < INDENT_STACK[INDENT_STACK.length - 1]!) {
        INDENT_STACK.pop();
        push(TokenKind.DEDENT);
      }
      if (INDENT_STACK[INDENT_STACK.length - 1] !== spaces) {
        Diagnostics.inconsistentDedent({ line, col }).throw();
      }
    }
  }

  // Skip UTF-8 BOM if present
  if (input.charCodeAt(0) === 0xfeff) {
    i++;
    col++;
  }

  while (i < input.length) {
    const ch = peek();

    // Line comments: '//' or '#'
    if (ch === '#') {
      emitCommentToken(1);
      continue;
    }
    if (ch === '/' && input[i + 1] === '/') {
      emitCommentToken(2);
      continue;
    }
    // Division operator (must come after '//' comment check)
    if (ch === '/') {
      const start = { line, col };
      next();
      push(TokenKind.SLASH, '/', start);
      continue;
    }

    // Newline + indentation (support \r\n and \r)
    if (ch === '\n' || ch === '\r') {
      // 保存位置在消费换行符之前
      const start = { line, col };
      if (ch === '\r') {
        next();
        if (peek() === '\n') next();
      } else {
        next();
      }
      push(TokenKind.NEWLINE, null, start);
      // Measure indentation
      let spaces = 0;
      let k = i;
      while (input[k] === ' ') {
        spaces++;
        k++;
      }
      // 跳过空行（包含 LF、CR、CRLF 换行格式）
      if (isLineBreak(input[k] || '') || k >= input.length) {
        i = k;
        continue;
      }
      if (input[k] === '#' || (input[k] === '/' && input[k + 1] === '/')) {
        i = k;
        col += spaces;
        continue;
      }
      emitIndentDedent(spaces);
      i = k;
      col += spaces;
      continue;
    }

    // Whitespace
    if (ch === ' ' || ch === '\t') {
      next();
      continue;
    }

    // Punctuation
    // 单字符 token 需要先保存位置再调用 next()
    if (ch === '.') {
      const start = { line, col };
      next();
      push(TokenKind.DOT, '.', start);
      continue;
    }
    if (ch === ':') {
      const start = { line, col };
      next();
      push(TokenKind.COLON, ':', start);
      continue;
    }
    if (ch === ',') {
      const start = { line, col };
      next();
      push(TokenKind.COMMA, ',', start);
      continue;
    }
    if (ch === '(') {
      const start = { line, col };
      next();
      push(TokenKind.LPAREN, '(', start);
      continue;
    }
    if (ch === ')') {
      const start = { line, col };
      next();
      push(TokenKind.RPAREN, ')', start);
      continue;
    }
    if (ch === '[') {
      const start = { line, col };
      next();
      push(TokenKind.LBRACKET, '[', start);
      continue;
    }
    if (ch === ']') {
      const start = { line, col };
      next();
      push(TokenKind.RBRACKET, ']', start);
      continue;
    }
    if (ch === '!') {
      const start = { line, col };
      next();
      if (peek() === '=') {
        next();
        push(TokenKind.NEQ, '!=', start);
      } else {
        Diagnostics.unexpectedCharacter(ch, { line, col }).throw();
      }
      continue;
    }
    if (ch === '=') {
      const start = { line, col };
      next();
      push(TokenKind.EQUALS, '=', start);
      continue;
    }
    if (ch === '+') {
      const start = { line, col };
      next();
      push(TokenKind.PLUS, '+', start);
      continue;
    }
    if (ch === '*') {
      const start = { line, col };
      next();
      push(TokenKind.STAR, '*', start);
      continue;
    }
    if (ch === '?') {
      const start = { line, col };
      next();
      push(TokenKind.QUESTION, '?', start);
      continue;
    }
    if (ch === '@') {
      const start = { line, col };
      next();
      push(TokenKind.AT, '@', start);
      continue;
    }
    if (ch === '-') {
      const start = { line, col };
      next();
      push(TokenKind.MINUS, '-', start);
      continue;
    }
    if (ch === '<') {
      const start = { line, col };
      next();
      if (peek() === '=') {
        next();
        push(TokenKind.LTE, '<=', start);
      } else {
        push(TokenKind.LT, '<', start);
      }
      continue;
    }
    if (ch === '>') {
      const start = { line, col };
      next();
      if (peek() === '=') {
        next();
        push(TokenKind.GTE, '>=', start);
      } else {
        push(TokenKind.GT, '>', start);
      }
      continue;
    }

    // String literal
    if (ch === '"') {
      const start = { line, col };
      next();
      let val = '';
      while (i < input.length && peek() !== '"') {
        if (peek() === '\\') {
          next();
          val += next();
        } else {
          val += next();
        }
      }
      if (peek() !== '"') Diagnostics.unterminatedString(start).throw();
      next(); // closing quote
      push(TokenKind.STRING, val, start);
      continue;
    }

    // Identifiers / numbers / keywords
    if (isLetter(ch)) {
      const start = { line, col };
      let word = '';
      while (isLetter(peek()) || isDigit(peek()) || peek() === '_') {
        word += next();
      }
      const lower = word.toLowerCase();
      // Handle booleans/null specially
      if (lower === KW.TRUE) {
        push(TokenKind.BOOL, true, start);
        continue;
      }
      if (lower === KW.FALSE) {
        push(TokenKind.BOOL, false, start);
        continue;
      }
      if (lower === KW.NULL) {
        push(TokenKind.NULL, null, start);
        continue;
      }
      // Types by capitalized first letter considered TYPE_IDENT
      if (/^[A-Z]/.test(word)) {
        push(TokenKind.TYPE_IDENT, word, start);
      } else {
        push(TokenKind.IDENT, word, start);
      }
      continue;
    }

    if (isDigit(ch)) {
      const start = { line, col };
      let num = '';
      while (isDigit(peek())) num += next();
      // Look for decimal part
      if (peek() === '.' && /\d/.test(input[i + 1] || '')) {
        num += next(); // '.'
        while (isDigit(peek())) num += next();
        const val = parseFloat(num);
        push(TokenKind.FLOAT, val, start);
        continue;
      }
      // Look for long suffix 'L' or 'l'
      if (peek().toLowerCase() === 'l') {
        next();
        // 使用 BigInt 避免精度损失，然后转换为 string
        const val = BigInt(num).toString();
        push(TokenKind.LONG, val, start);
        continue;
      }
      push(TokenKind.INT, parseInt(num, 10), start);
      continue;
    }

    Diagnostics.unexpectedCharacter(ch, { line, col }).throw();
  }

  // Close indentation stack
  while (INDENT_STACK.length > 1) {
    INDENT_STACK.pop();
    push(TokenKind.DEDENT);
  }
  push(TokenKind.EOF);
  return tokens;
}
