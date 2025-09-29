import { TokenKind, KW } from './tokens.js';
import type { Token, Position } from './types.js';
import { Diagnostics } from './diagnostics.js';

const KW_VALUES = new Set(Object.values(KW) as string[]);

function isLetter(ch: string): boolean {
  return /[A-Za-z]/.test(ch);
}

function isDigit(ch: string): boolean {
  return /[0-9]/.test(ch);
}

export function lex(input: string): Token[] {
  const tokens: Token[] = [];
  let i = 0;
  let line = 1;
  let col = 1;

  const push = (
    kind: TokenKind,
    value: string | number | boolean | null = null,
    start: Position = { line, col }
  ): void => {
    tokens.push({ kind, value, start, end: { line, col } });
  };

  const peek = (): string => input[i] || '';
  const next = (): string => {
    const ch = input[i++] || '';
    if (ch === '\n') {
      line++;
      col = 1;
    } else {
      col++;
    }
    return ch;
  };

  const INDENT_STACK = [0];

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
      // consume until newline (do not emit tokens)
      while (i < input.length && peek() !== '\n') next();
      continue;
    }
    if (ch === '/' && input[i + 1] === '/') {
      // consume '//' and rest of the line
      next();
      next();
      while (i < input.length && peek() !== '\n') next();
      continue;
    }

    // Newline + indentation (support \r\n and \r)
    if (ch === '\n' || ch === '\r') {
      if (ch === '\r') {
        next();
        if (peek() === '\n') next();
      } else {
        next();
      }
      push(TokenKind.NEWLINE);
      // Measure indentation
      let spaces = 0;
      let k = i;
      while (input[k] === ' ') {
        spaces++;
        k++;
      }
      if (input[k] === '\n' || k >= input.length) {
        i = k;
        continue;
      }
      // Only treat indentation if next token is not comment; (no comments yet)
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
    if (ch === '.') {
      next();
      push(TokenKind.DOT, '.');
      continue;
    }
    if (ch === ':') {
      next();
      push(TokenKind.COLON, ':');
      continue;
    }
    if (ch === ',') {
      next();
      push(TokenKind.COMMA, ',');
      continue;
    }
    if (ch === '(') {
      next();
      push(TokenKind.LPAREN, '(');
      continue;
    }
    if (ch === ')') {
      next();
      push(TokenKind.RPAREN, ')');
      continue;
    }
    if (ch === '=') {
      next();
      push(TokenKind.EQUALS, '=');
      continue;
    }
    if (ch === '+') {
      next();
      push(TokenKind.PLUS, '+');
      continue;
    }
    if (ch === '?') {
      next();
      push(TokenKind.QUESTION, '?');
      continue;
    }
    if (ch === '-') {
      next();
      push(TokenKind.MINUS, '-');
      continue;
    }
    if (ch === '<') {
      next();
      push(TokenKind.LT, '<');
      continue;
    }
    if (ch === '>') {
      next();
      push(TokenKind.GT, '>');
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
      // Keywords (case-insensitive) are emitted as IDENT with their source casing preserved
      if (KW_VALUES.has(lower)) {
        push(TokenKind.IDENT, word, start);
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
        const val = parseInt(num, 10);
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
