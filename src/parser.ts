/**
 * Aster Language Parser - 主入口
 * 负责协调各个子模块完成整个模块的解析
 */

import { KW, TokenKind } from './tokens.js';
import { Node } from './ast.js';
import { ConfigService } from './config/config-service.js';
import type { Declaration, Module, Token } from './types.js';
import { Diagnostics } from './diagnostics.js';
import { createLogger } from './utils/logger.js';
import type { ParserContext } from './parser/context.js';
import { kwParts, tokLowerAt } from './parser/context.js';
import { parseModuleHeader, parseImport } from './parser/import-parser.js';
import { parseFuncDecl } from './parser/decl-parser.js';
import { parseFieldList, parseVariantList } from './parser/field-variant-parser.js';

const parserLogger = createLogger('parser');

/**
 * 解析标记流生成 AST
 * @param tokens 词法标记数组
 * @returns 模块 AST
 */
export function parse(tokens: readonly Token[]): Module {
  // 创建解析器上下文
  const ctx: ParserContext = {
    tokens,
    index: 0,
    moduleName: null,
    declaredTypes: new Set<string>(),
    currentTypeVars: new Set<string>(),
    collectedEffects: null,
    effectSnapshots: [],
    debug: {
      enabled: ConfigService.getInstance().debugTypes,
      depth: 0,
      log: (message: string): void => {
        if (!ctx.debug.enabled) return;
        parserLogger.debug(`[parseType] ${message}`, { depth: ctx.debug.depth });
      },
    },
    peek: (offset: number = 0): Token => {
      const idx = ctx.index + offset;
      if (idx < ctx.tokens.length) return ctx.tokens[idx]!;
      return ctx.tokens[ctx.tokens.length - 1]!;
    },
    next: (): Token => {
      const tok = ctx.peek();
      if (ctx.index < ctx.tokens.length) ctx.index += 1;
      return tok;
    },
    at: (kind: TokenKind, value?: Token['value']): boolean => {
      const t = ctx.peek();
      if (!t) return false;
      if (t.kind !== kind) return false;
      if (value === undefined) return true;
      return t.value === value;
    },
    expect: (kind: TokenKind, message: string): Token => {
      if (!ctx.at(kind)) error(message + `, got ${ctx.peek().kind}`);
      return ctx.next();
    },
    isKeyword: (kw: string): boolean => {
      const v = tokLowerAt(ctx, ctx.index);
      return v === kw;
    },
    isKeywordSeq: (words: string | string[]): boolean => {
      const ws = Array.isArray(words) ? words : kwParts(words);
      for (let k = 0; k < ws.length; k++) {
        const v = tokLowerAt(ctx, ctx.index + k);
        if (v !== ws[k]) return false;
      }
      return true;
    },
    nextWord: (): Token => {
      if (!(ctx.at(TokenKind.IDENT) || ctx.at(TokenKind.TYPE_IDENT)))
        error('Expected keyword/identifier');
      return ctx.next();
    },
    nextWords: (words: string[]): void => {
      words.forEach(() => ctx.nextWord());
    },
    consumeIndent: (): void => {
      while (ctx.at(TokenKind.INDENT)) ctx.next();
    },
    consumeNewlines: (): void => {
      while (ctx.at(TokenKind.NEWLINE)) ctx.next();
    },
    pushEffect: (effects: string[]): void => {
      if (!Array.isArray(ctx.collectedEffects)) ctx.collectedEffects = [];
      ctx.collectedEffects!.push(...effects);
    },
    snapshotEffects: (): string[] | null => {
      const snapshot = ctx.collectedEffects ? [...ctx.collectedEffects] : null;
      ctx.effectSnapshots.push(snapshot);
      return snapshot;
    },
    restoreEffects: (snapshot: string[] | null): void => {
      ctx.collectedEffects = snapshot ? [...snapshot] : null;
    },
    withTypeScope: <T>(names: Iterable<string>, body: () => T): T => {
      const saved = new Set(ctx.currentTypeVars);
      for (const name of names) ctx.currentTypeVars.add(name);
      try {
        return body();
      } finally {
        ctx.currentTypeVars = saved;
      }
    },
  };

  // 错误报告辅助函数
  function error(msg: string, tok: Token = ctx.peek()): never {
    Diagnostics.unexpectedToken(msg, tok.start).withMessage(msg).throw();
    throw new Error('unreachable');
  }

  // 期望关键字辅助函数
  function expectKeyword(kw: string, msg: string): void {
    if (!ctx.isKeyword(kw))
      Diagnostics.expectedKeyword(kw, ctx.peek().start).withMessage(msg).throw();
    ctx.nextWord();
  }

  // 期望点号辅助函数
  function expectDot(): void {
    if (!ctx.at(TokenKind.DOT))
      Diagnostics.expectedPunctuation('.', ctx.peek().start).throw();
    ctx.next();
  }

  // 期望逗号或允许省略辅助函数
  function expectCommaOr(): void {
    if (ctx.at(TokenKind.COMMA)) {
      ctx.next();
    }
  }

  // 期望换行辅助函数
  function expectNewline(): void {
    if (!ctx.at(TokenKind.NEWLINE))
      Diagnostics.expectedToken('newline', ctx.peek().kind, ctx.peek().start).throw();
    ctx.next();
  }

  // 解析标识符辅助函数
  function parseIdent(): string {
    if (!ctx.at(TokenKind.IDENT))
      Diagnostics.expectedIdentifier(ctx.peek().start).throw();
    return ctx.next().value as string;
  }

  // 解析类型标识符辅助函数
  function parseTypeIdent(): string {
    if (!ctx.at(TokenKind.TYPE_IDENT))
      Diagnostics.expectedToken('Type identifier', ctx.peek().kind, ctx.peek().start).throw();
    return ctx.next().value as string;
  }

  // 主解析循环：收集顶层声明
  const decls: Declaration[] = [];
  ctx.consumeNewlines();

  while (!ctx.at(TokenKind.EOF)) {
    ctx.consumeNewlines();
    while (ctx.at(TokenKind.DEDENT)) ctx.next();
    while (ctx.at(TokenKind.INDENT)) ctx.next();
    ctx.consumeNewlines();
    if (ctx.at(TokenKind.EOF)) break;

    // 解析模块头: This module is foo.bar.
    if (ctx.isKeywordSeq(KW.MODULE_IS)) {
      parseModuleHeader(ctx, error, expectDot);
    }
    // 解析导入: use foo.bar. 或 use foo.bar as Baz.
    else if (ctx.isKeyword(KW.USE)) {
      const { name, asName } = parseImport(ctx, error, expectDot, parseIdent);
      decls.push(Node.Import(name, asName));
    }
    // 解析类型定义: Define ...
    else if (ctx.isKeyword(KW.DEFINE)) {
      // 消费 'Define' 关键字
      ctx.nextWord();
      // 解析类型名
      const typeName = parseTypeIdent();

      // 判断是 Data 还是 Enum
      if (ctx.isKeywordSeq(KW.WITH)) {
        // Data: Define User with ...
        ctx.nextWord();
        const fields = parseFieldList(ctx, error);
        expectDot();
        const dataDecl = Node.Data(typeName, fields);
        ctx.declaredTypes.add(typeName);
        decls.push(dataDecl);
      } else if (ctx.isKeywordSeq(KW.ONE_OF)) {
        // Enum: Define Status as one of ...
        ctx.nextWords(kwParts(KW.ONE_OF));
        const variants = parseVariantList(ctx, error);
        expectDot();
        const en = Node.Enum(typeName, variants);
        const spans = (parseVariantList as any)._lastSpans as import('./types.js').Span[] | undefined;
        if (spans && Array.isArray(spans)) {
          (en as any).variantSpans = spans;
        }
        ctx.declaredTypes.add(typeName);
        decls.push(en);
      } else {
        error("Expected 'with' or 'as one of' after type name");
      }
    }
    // 解析函数: To ...
    else if (ctx.isKeyword(KW.TO)) {
      decls.push(parseFuncDecl(ctx, error, expectCommaOr, expectKeyword, expectNewline, parseIdent));
    }
    // 容忍顶层的空白/缩进/反缩进
    else if (ctx.at(TokenKind.NEWLINE) || ctx.at(TokenKind.DEDENT) || ctx.at(TokenKind.INDENT)) {
      ctx.next();
    }
    // 其他情况报错
    else {
      error('Unexpected token at top level');
    }

    ctx.consumeNewlines();
  }

  return Node.Module(ctx.moduleName, decls);
}
