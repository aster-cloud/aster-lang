import type { Token } from '../types.js';
import { TokenKind } from '../tokens.js';

/**
 * Parser 上下文接口
 * 包含词法标记流和解析状态
 */
export interface ParserContext {
  readonly tokens: readonly Token[];
  index: number;
  moduleName: string | null;
  declaredTypes: Set<string>;
  currentTypeVars: Set<string>;
  collectedEffects: string[] | null;
  effectSnapshots: Array<string[] | null>;
  debug: { enabled: boolean; depth: number; log(message: string): void };
  peek(offset?: number): Token;
  next(): Token;
  at(kind: TokenKind, value?: Token['value']): boolean;
  expect(kind: TokenKind, message: string): Token;
  isKeyword(kw: string): boolean;
  isKeywordSeq(words: string | string[]): boolean;
  nextWord(): Token;
  nextWords(words: string[]): void;
  consumeIndent(): void;
  consumeNewlines(): void;
  pushEffect(effects: string[]): void;
  snapshotEffects(): string[] | null;
  restoreEffects(snapshot: string[] | null): void;
  withTypeScope<T>(names: Iterable<string>, body: () => T): T;
}

/**
 * 将关键字短语拆分为单词数组
 * @param phrase 关键字短语（如 "module is"）
 * @returns 单词数组
 */
export function kwParts(phrase: string): string[] {
  return phrase.split(' ');
}

/**
 * 获取指定位置的词法标记的小写值
 * @param ctx Parser 上下文
 * @param idx 标记索引
 * @returns 小写的标记值，如果不是标识符则返回 null
 */
export function tokLowerAt(ctx: ParserContext, idx: number): string | null {
  const t = ctx.tokens[idx];
  if (!t) return null;
  if (t.kind !== TokenKind.IDENT && t.kind !== TokenKind.TYPE_IDENT) return null;
  return ((t.value as string) || '').toLowerCase();
}
