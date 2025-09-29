import { canonicalize } from './canonicalizer.js';
import { lex } from './lexer.js';
import type { Token } from './types.js';
import type { CstModule, CstToken } from './cst.js';

function buildLineStarts(text: string): number[] {
  const starts: number[] = [0];
  for (let i = 0; i < text.length; i++) if (text[i] === '\n') starts.push(i + 1);
  return starts;
}

function toOffset(starts: readonly number[], line: number, col: number): number {
  const li = Math.max(1, line) - 1;
  const base = starts[li] ?? 0;
  return base + Math.max(1, col) - 1;
}

function tokensToCstTokens(text: string, tokens: readonly Token[]): CstToken[] {
  const starts = buildLineStarts(text);
  const res: CstToken[] = [];
  for (const t of tokens) {
    const startOffset = toOffset(starts, t.start.line, t.start.col);
    const endOffset = toOffset(starts, t.end.line, t.end.col);
    const lexeme = text.slice(startOffset, endOffset);
    res.push({ kind: t.kind, lexeme, start: t.start, end: t.end, startOffset, endOffset });
  }
  return res;
}

export function buildCst(text: string, prelexed?: readonly Token[]): CstModule {
  const can = canonicalize(text);
  const toks = (prelexed as Token[] | undefined) ?? lex(can);
  const cstTokens = tokensToCstTokens(text, toks);
  const span = cstTokens.length
    ? { start: cstTokens[0]!.start, end: cstTokens[cstTokens.length - 1]!.end }
    : { start: { line: 1, col: 1 }, end: { line: 1, col: 1 } };
  const leading = cstTokens.length > 0 ? { text: text.slice(0, cstTokens[0]!.startOffset) } : { text: text };
  const trailing = cstTokens.length > 0
    ? { text: text.slice(cstTokens[cstTokens.length - 1]!.endOffset) }
    : { text: '' };
  return { kind: 'Module', tokens: cstTokens, children: [], span, leading, trailing } as CstModule;
}
