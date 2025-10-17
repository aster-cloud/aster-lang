/**
 * 字段和变体列表解析器
 * 负责解析数据类型的字段列表和枚举类型的变体列表
 */

import { KW, TokenKind } from '../tokens.js';
import type { Field, Span, Token } from '../types.js';
import type { ParserContext } from './context.js';
import { parseType } from './type-parser.js';
import { parseAnnotations } from './annotation-parser.js';

/**
 * 解析字段列表（用于 Data 类型定义）
 * 语法: field1: Type1, field2: Type2 and field3: Type3
 *
 * @param ctx 解析器上下文
 * @param error 错误报告函数
 * @returns 字段数组
 */
export function parseFieldList(
  ctx: ParserContext,
  error: (msg: string, tok?: Token) => never
): Field[] {
  const fields: Field[] = [];
  let hasMore = true;

  while (hasMore) {
    // 在开始解析字段前，先消费换行和缩进，支持多行格式
    ctx.consumeNewlines();
    ctx.consumeIndent();

    const { annotations, firstToken } = parseAnnotations(ctx, error);
    const nameTok = ctx.peek();

    // 解析字段名（必须是普通标识符）
    if (!ctx.at(TokenKind.IDENT)) {
      error("Expected field name", nameTok);
    }
    const name = ctx.next().value as string;

    // 期望冒号
    if (!ctx.at(TokenKind.COLON)) {
      error("Expected ':' after field name");
    }
    ctx.next();

    // 解析字段类型
    const t = parseType(ctx, error);

    // 创建字段对象并附加 span
    const f: Field =
      annotations.length > 0 ? { name, type: t, annotations } : { name, type: t };
    const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
    const spanStart = annotations.length > 0 && firstToken ? firstToken.start : nameTok.start;
    (f as any).span = { start: spanStart, end: endTok.end };
    fields.push(f);

    // 检查是否还有更多字段
    if (ctx.at(TokenKind.COMMA)) {
      ctx.next();
      // 逗号后允许换行和缩进
      ctx.consumeNewlines();
      ctx.consumeIndent();
      continue;
    }
    if (ctx.isKeyword(KW.AND)) {
      ctx.nextWord();
      // 'and' 后允许换行和缩进
      ctx.consumeNewlines();
      ctx.consumeIndent();
      continue;
    }
    hasMore = false;
  }

  return fields;
}

/**
 * 解析变体列表（用于 Enum 类型定义）
 * 语法: Variant1, Variant2 or Variant3
 *
 * @param ctx 解析器上下文
 * @param error 错误报告函数
 * @returns 变体名称数组
 *
 * 注意：变体的 Span 信息通过副作用存储在函数对象上
 * 调用者需要通过 (parseVariantList as any)._lastSpans 获取
 */
export function parseVariantList(
  ctx: ParserContext,
  error: (msg: string, tok?: Token) => never
): string[] {
  const vars: string[] = [];
  const spans: Span[] = [];
  let hasMore = true;

  while (hasMore) {
    const vTok = ctx.peek();

    // 解析变体名（必须是类型标识符）
    if (!ctx.at(TokenKind.TYPE_IDENT)) {
      error("Expected type identifier for variant name", vTok);
    }
    const v = ctx.next().value as string;

    // 记录变体的 span
    const endTok = ctx.tokens[ctx.index - 1] || vTok;
    spans.push({ start: vTok.start, end: endTok.end });
    vars.push(v);

    // 检查是否还有更多变体
    if (ctx.at(TokenKind.IDENT) && ((ctx.peek().value as string) || '').toLowerCase() === KW.OR) {
      ctx.nextWord();
      continue;
    }
    if (ctx.at(TokenKind.COMMA)) {
      ctx.next();
      continue;
    }
    hasMore = false;
  }

  // 使用副作用存储 spans（这是一个技术债务，但保持与现有代码兼容）
  (parseVariantList as any)._lastSpans = spans;
  return vars;
}
