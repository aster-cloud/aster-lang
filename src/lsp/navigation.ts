/**
 * LSP Navigation 模块
 * 提供代码导航功能：引用查找、重命名、悬停提示、符号树、定义跳转
 */

import type {
  Connection,
  ReferenceParams,
  RenameParams,
  Location,
  WorkspaceEdit,
  DocumentSymbol,
  DocumentSymbolParams,
  ProgressToken
} from 'vscode-languageserver/node.js';

/**
 * LSP 参数类型扩展，包含 progress token 字段
 */
interface ParamsWithProgress {
  workDoneToken?: ProgressToken;
  partialResultToken?: ProgressToken;
}
import { SymbolKind } from 'vscode-languageserver/node.js';
import { TextDocument } from 'vscode-languageserver-textdocument';
import { typeText } from './completion.js';
import { findSymbolReferences, getAllModules, updateDocumentIndex } from './index.js';
import {
  findDottedCallRangeAt,
  describeDottedCallAt,
  findAmbiguousInteropCalls,
  buildDescriptorPreview,
  returnTypeTextFromDesc,
} from './analysis.js';
import { exprTypeText } from './utils.js';
import { parse } from '../parser.js';
import { canonicalize } from '../canonicalizer.js';
import { lex } from '../lexer.js';
import { TokenKind } from '../tokens.js';
import {
  getSpan,
  getNameSpan,
  getVariantSpans,
  getStatements,
  isAstFunc,
  isAstData,
  isAstEnum,
  isAstBlock,
} from './type-guards.js';
import type {
  Module as AstModule,
  Declaration as AstDecl,
  Func as AstFunc,
  Data as AstData,
  Enum as AstEnum,
  Block as AstBlock,
  Span,
} from '../types.js';
import { promises as fsPromises } from 'node:fs';
import { buildTokenIndex, tokenNameAt as tokenNameAtOptimized } from './token-index.js';

/**
 * 捕获指定偏移量处的单词
 * @param text 文本内容
 * @param offset 偏移量位置
 * @returns 捕获的单词，如果不在单词位置则返回 null
 */
function captureWordAt(text: string, offset: number): string | null {
  const isWord = (c: string): boolean => /[A-Za-z0-9_.]/.test(c);
  let s = offset;
  while (s > 0 && isWord(text[s - 1]!)) s--;
  let e = offset;
  while (e < text.length && isWord(text[e]!)) e++;
  if (s === e) return null;
  return text.slice(s, e);
}

/**
 * 查找文本中所有单词出现位置
 * @param text 文本内容
 * @param word 要查找的单词
 * @returns 单词位置数组（包含 start 和 end 偏移量）
 */
function findWordPositions(text: string, word: string): Array<{ start: number; end: number }> {
  const out: Array<{ start: number; end: number }> = [];
  const re = new RegExp(`(?<![A-Za-z0-9_.])${word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(?![A-Za-z0-9_.])`, 'g');
  for (let m; (m = re.exec(text)); ) {
    out.push({ start: m.index, end: m.index + word.length });
  }
  return out;
}

/**
 * 安全查找标记位置（支持回退到正则匹配）
 * @param text 文本内容
 * @param word 要查找的标记或单词
 * @returns 标记位置数组，如果词法分析失败则回退到正则匹配
 */
export function findTokenPositionsSafe(text: string, word: string): Array<{ start: number; end: number }> {
  // If word contains '.', fallback to simple word regex matching over text
  if (word.includes('.')) return findWordPositions(text, word);
  try {
    const can = canonicalize(text);
    const toks = lex(can);
    const starts = buildLineStarts(text);
    const out: Array<{ start: number; end: number }> = [];
    for (const t of toks) {
      if ((t.kind === TokenKind.IDENT || t.kind === TokenKind.TYPE_IDENT) && String(t.value) === word) {
        const s = toOffset(starts, t.start.line, t.start.col);
        const e = toOffset(starts, t.end.line, t.end.col);
        out.push({ start: s, end: e });
      }
    }
    return out.length ? out : findWordPositions(text, word);
  } catch {
    return findWordPositions(text, word);
  }
}

/**
 * 构建行起始位置数组
 * @param text 文本内容
 * @returns 每行起始偏移量数组
 */
function buildLineStarts(text: string): number[] {
  const a: number[] = [0];
  for (let i = 0; i < text.length; i++) if (text[i] === '\n') a.push(i + 1);
  return a;
}

/**
 * 将 AST 坐标（1-based 行列）转换为文本偏移量
 * @param starts 行起始位置数组
 * @param line AST 行号（1-based）
 * @param col AST 列号（1-based）
 * @returns 文本偏移量
 */
function toOffset(starts: readonly number[], line: number, col: number): number {
  const li = Math.max(1, line) - 1;
  const base = starts[li] ?? 0;
  return base + Math.max(1, col) - 1;
}

/**
 * 将文本偏移量转换为 LSP Position（0-based 行列）
 * @param text 文本内容
 * @param off 偏移量
 * @returns LSP Position 对象
 */
export function offsetToPos(text: string, off: number): { line: number; character: number } {
  let line = 0;
  let last = 0;
  for (let i = 0; i < text.length && i < off; i++) if (text[i] === '\n') { line++; last = i + 1; }
  return { line, character: off - last };
}

/**
 * 确保路径为 file:// URI 格式
 * @param u 文件路径或 URI
 * @returns file:// URI 字符串
 */
export function ensureUri(u: string): string {
  if (u.startsWith('file://')) return u;
  const path = require('node:path');
  const to = 'file://' + (path.isAbsolute(u) ? u : path.join(process.cwd(), u));
  return to;
}

/**
 * URI 转文件系统路径
 * @param u file:// URI 字符串
 * @returns 文件系统路径，转换失败返回 null
 */
export function uriToFsPath(u: string): string | null {
  try {
    if (u.startsWith('file://')) return new URL(u).pathname;
  } catch {}
  return null;
}

/**
 * 将 Span 转换为 Range，如果无 Span 则返回整个文档范围
 * @param span AST Span 对象（可选）
 * @param doc 文档对象
 * @returns LSP Range 对象
 */
function spanOrDoc(span: Span | undefined, doc: TextDocument): import('vscode-languageserver/node.js').Range {
  if (span) {
    return {
      start: { line: Math.max(0, span.start.line - 1), character: Math.max(0, span.start.col - 1) },
      end: { line: Math.max(0, span.end.line - 1), character: Math.max(0, span.end.col - 1) },
    };
  }
  const last = doc.lineCount - 1;
  const len = doc.getText({ start: { line: last, character: 0 }, end: { line: last, character: Number.MAX_SAFE_INTEGER } }).length;
  return { start: { line: 0, character: 0 }, end: { line: last, character: len } };
}

/**
 * 生成函数详情字符串（参数和返回类型）
 * @param f 函数 AST 节点
 * @returns 函数签名字符串，包含参数、返回类型和效果
 */
function funcDetail(f: AstFunc): string {
  const eff = (f.effects || []).join(' ');
  const params = f.params.map(p => `${p.name}: ${typeText(p.type)}`).join(', ');
  const ret = typeText(f.retType);
  const effTxt = eff ? ` performs ${eff}` : '';
  return `(${params}) -> ${ret}${effTxt}`;
}

/**
 * 判断 LSP Position 是否在 Span 范围内
 * @param span AST Span 对象（可选）
 * @param pos LSP Position（0-based 行列）
 * @returns 是否在范围内
 */
function within(span: Span | undefined, pos: { line: number; character: number }): boolean {
  if (!span) return false;
  const l = pos.line + 1, c = pos.character + 1;
  const s = span.start, e = span.end;
  if (l < s.line || l > e.line) return false;
  if (l === s.line && c < s.col) return false;
  return !(l === e.line && c > e.col);
}

/**
 * 查找指定位置处的顶层声明
 * @param m 模块 AST
 * @param pos LSP Position
 * @returns 匹配的声明节点，优先返回函数声明
 */
function findDeclAt(m: AstModule, pos: { line: number; character: number }): AstDecl | null {
  let found: AstDecl | null = null;
  for (const d of m.decls) {
    const sp: Span | undefined = getSpan(d);
    if (within(sp, pos)) {
      found = d as AstDecl;
      if (isAstFunc(d)) return d;
    }
  }
  return found;
}

/**
 * 获取指定位置处的标记名称
 * @param tokens 词法标记数组
 * @param pos LSP Position
 * @returns 标记名称（标识符或类型标识符），如果未找到则返回 null
 */
export function tokenNameAt(tokens: readonly any[], pos: { line: number; character: number }): string | null {
  for (const t of tokens) {
    if (!t || !t.start || !t.end) continue;
    const span: Span = { start: { line: t.start.line, col: t.start.col }, end: { line: t.end.line, col: t.end.col } };
    if (within(span, pos)) {
      if (t.kind === 'IDENT' || t.kind === 'TYPE_IDENT') return String(t.value || '');
    }
  }
  return null;
}

/**
 * 将 Span 转换为 LSP Location 对象
 * @param uri 文档 URI
 * @param sp AST Span 对象
 * @returns LSP Location 对象
 */
function toLocation(uri: string, sp: Span): Location {
  return {
    uri,
    range: {
      start: { line: sp.start.line - 1, character: sp.start.col - 1 },
      end: { line: sp.end.line - 1, character: sp.end.col - 1 },
    },
  };
}

/**
 * 查找模式匹配绑定的详细信息
 * @param fn 函数 AST 节点
 * @param name 绑定名称
 * @param pos LSP Position
 * @returns 绑定信息（包含名称和可选的类型），如果未找到则返回 null
 */
function findPatternBindingDetail(fn: AstFunc, name: string, pos: { line: number; character: number }): { name: string; ofType?: string | undefined } | null {
  const inRange = (sp?: Span): boolean => within(sp, pos);
  if (!fn.body) return null;
  const walkBlock = (b: AstBlock): { name: string; ofType?: string | undefined } | null => {
    const statements = getStatements(b);
    for (const s of statements) {
      if (s.kind === 'Match') {
        for (const c of s.cases) {
          const names: string[] = [];
          let ofType: string | undefined;
          const extract = (p: any): void => {
            if (!p) return;
            if (p.kind === 'PatternName') names.push(p.name);
            else if (p.kind === 'PatternCtor') {
              ofType = ofType || p.typeName;
              (p.names || []).forEach((n: string) => names.push(n));
              (p.args || []).forEach(extract);
            }
          };
          extract(c.pattern);
          const body = c.body;
          const sp = getSpan(body);
          if (names.includes(name) && inRange(sp)) {
            return ofType ? { name, ofType } : { name };
          }
          if (body && body.kind === 'Block') {
            const inner = walkBlock(body as AstBlock);
            if (inner) return inner;
          }
        }
      } else if (s.kind === 'If') {
        const a = walkBlock(s.thenBlock as AstBlock) || (s.elseBlock ? walkBlock(s.elseBlock as AstBlock) : null);
        if (a) return a;
      } else if (isAstBlock(s)) {
        const a = walkBlock(s);
        if (a) return a;
      }
    }
    return null;
  };
  return walkBlock(fn.body);
}

/**
 * 在函数体中查找局部 let 绑定及其表达式
 * @param b 块 AST 节点
 * @param name 绑定名称
 * @returns let 绑定信息（包含 span 和表达式），如果未找到则返回 null
 */
function findLocalLetWithExpr(b: AstBlock | null, name: string): { span: Span; expr: any } | null {
  if (!b) return null;
  const statements = getStatements(b);
  for (const s of statements) {
    if (s.kind === 'Let' && s.name === name) {
      const sp = getSpan(s);
      if (sp) return { span: sp, expr: s.expr };
    }
    if (s.kind === 'If') {
      const a = findLocalLetWithExpr(s.thenBlock as AstBlock, name);
      if (a) return a;
      if (s.elseBlock) {
        const b2 = findLocalLetWithExpr(s.elseBlock as AstBlock, name);
        if (b2) return b2;
      }
    } else if (s.kind === 'Match') {
      for (const c of s.cases) {
        if (c.body.kind === 'Block') {
          const r = findLocalLetWithExpr(c.body as AstBlock, name);
          if (r) return r;
        }
      }
    } else if (isAstBlock(s)) {
      const r = findLocalLetWithExpr(s, name);
      if (r) return r;
    }
  }
  return null;
}

/**
 * 收集函数体中所有 let 绑定及其 Span
 * @param b 块 AST 节点
 * @returns 绑定名称到 Span 的映射
 */
export function collectLetsWithSpan(b: AstBlock | null): Map<string, Span> {
  const out = new Map<string, Span>();
  if (!b) return out;
  const statements = getStatements(b);
  for (const s of statements) {
    if (s.kind === 'Let') {
      const sp = getSpan(s);
      if (sp) out.set(s.name, sp);
    } else if (s.kind === 'If') {
      collectLetsWithSpan(s.thenBlock as AstBlock).forEach((v, k) => out.set(k, v));
      if (s.elseBlock) collectLetsWithSpan(s.elseBlock as AstBlock).forEach((v, k) => out.set(k, v));
    } else if (s.kind === 'Match') {
      for (const c of s.cases) if (c.body.kind === 'Block') collectLetsWithSpan(c.body as AstBlock).forEach((v, k) => out.set(k, v));
    } else if (isAstBlock(s)) {
      collectLetsWithSpan(s).forEach((v, k) => out.set(k, v));
    }
  }
  return out;
}

/**
 * 构建枚举变体名称到 Span 的映射
 * @param m 模块 AST
 * @returns 变体名称到 Span 的映射
 */
function enumVariantSpanMap(m: AstModule): Map<string, Span> {
  const out = new Map<string, Span>();
  for (const d of m.decls as AstDecl[]) {
    if (isAstEnum(d)) {
      const vspans: (Span | undefined)[] = getVariantSpans(d);
      for (let i = 0; i < d.variants.length; i++) {
        const nm = d.variants[i]!;
        const sp = vspans[i];
        if (sp) out.set(nm, sp);
      }
    }
  }
  return out;
}

/**
 * 构建数据字段到 Span 的映射（key: TypeName.field）
 * @param m 模块 AST
 * @returns 字段键（TypeName.field）到 Span 的映射
 */
function dataFieldSpanMap(m: AstModule): Map<string, Span> {
  const out = new Map<string, Span>();
  for (const d of m.decls as AstDecl[]) {
    if (isAstData(d)) {
      for (const f of d.fields) {
        const sp = getSpan(f);
        if (sp) out.set(`${d.name}.${f.name}`, sp);
      }
    }
  }
  return out;
}

/**
 * 查找指定位置处的构造字段信息
 * @param m 模块 AST
 * @param pos LSP Position
 * @returns 构造字段信息（包含类型名和字段名），如果未找到则返回 null
 */
function findConstructFieldAt(m: AstModule, pos: { line: number; character: number }): { typeName: string; field: string } | null {
  // Shallow walk function bodies to find Construct nodes and match field spans
  const withinSpan = (sp: Span | undefined): boolean => within(sp, pos);
  for (const d of m.decls as AstDecl[]) {
    if (!isAstFunc(d)) continue;
    const f = d;
    const walkBlock = (b: AstBlock): void => {
      const statements = getStatements(b);
      for (const s of statements) {
        if (s.kind === 'Return') {
          walkExpr(s.expr);
        } else if (s.kind === 'Let' || s.kind === 'Set') {
          walkExpr(s.expr);
        } else if (s.kind === 'If') {
          walkExpr(s.cond);
          walkBlock(s.thenBlock as AstBlock);
          if (s.elseBlock) walkBlock(s.elseBlock as AstBlock);
        } else if (s.kind === 'Match') {
          walkExpr(s.expr);
          for (const c of s.cases) {
            if (c.body.kind === 'Block') walkBlock(c.body as AstBlock);
          }
        } else if (isAstBlock(s)) {
          walkBlock(s);
        }
      }
    };
    const walkExpr = (e: any): void => {
      if (!e || !e.kind) return;
      if (e.kind === 'Construct') {
        for (const fld of e.fields || []) {
          const sp = getSpan(fld);
          if (withinSpan(sp)) { found = { typeName: e.typeName as string, field: fld.name as string }; return; }
        }
      } else if (e.kind === 'Call') {
        walkExpr(e.target);
        (e.args || []).forEach(walkExpr);
      } else if (e.kind === 'Ok' || e.kind === 'Err' || e.kind === 'Some') {
        walkExpr(e.expr);
      }
    };
    let found: { typeName: string; field: string } | null = null;
    if (f.body) walkBlock(f.body);
    if (found) return found;
  }
  return null;
}

/**
 * 注册 Navigation 相关的 LSP 处理器
 * @param connection LSP 连接对象
 * @param documents 文档管理器，提供 get 方法按 URI 获取文档
 * @param getOrParse 文档解析函数，返回文本、词法标记和 AST
 * @param getDocumentSettings 获取文档设置的函数
 */
export function registerNavigationHandlers(
  connection: Connection,
  documents: { get(uri: string): TextDocument | undefined; keys(): string[] },
  getOrParse: (doc: TextDocument) => { text: string; tokens: readonly any[]; ast: any },
  getDocumentSettings: (uri: string) => Promise<any>
): void {
  // Progress 闭包函数（访问 connection 对象）
  const _beginProgress = (token: ProgressToken | undefined, title: string): void => {
    try {
      if (!token) return;
      (connection as any).sendProgress('$/progress', token, { kind: 'begin', title });
    } catch {
      // ignore
    }
  };

  const _reportProgress = (token: ProgressToken | undefined, message: string): void => {
    try {
      if (!token) return;
      (connection as any).sendProgress('$/progress', token, { kind: 'report', message });
    } catch {
      // ignore
    }
  };

  const _endProgress = (token: ProgressToken | undefined): void => {
    try {
      if (!token) return;
      (connection as any).sendProgress('$/progress', token, { kind: 'end' });
    } catch {
      // ignore
    }
  };

  const defaultSettings = { rename: { scope: 'workspace' }, streaming: { referencesChunk: 200, renameChunk: 200, logChunks: false } };

  // onReferences: 查找符号引用
  connection.onReferences(async (params: ReferenceParams & ParamsWithProgress, token?: any) => {
    const doc = documents.get(params.textDocument.uri);
    if (!doc) return [];
    const text = doc.getText();
    const pos = params.position;
    const offset = doc.offsetAt(pos);
    // naive token capture: expand to nearest word boundaries
    const word = captureWordAt(text, offset);
    if (!word) return [];
    const out: Location[] = [];
    const settings = await getDocumentSettings(doc.uri).catch(() => defaultSettings);
    const scope = settings.rename?.scope ?? 'workspace';
    const openUris = new Set(documents.keys());
    try { await updateDocumentIndex(doc.uri, doc.getText()); } catch {}
    const refs = await findSymbolReferences(word, undefined);
    const filtered = scope === 'open' ? refs.filter(loc => openUris.has(loc.uri)) : refs;
    _beginProgress(params.workDoneToken, 'Aster references');
    const CHUNK = settings.streaming?.referencesChunk ?? 200;
    let batch: Location[] = [];
    for (const loc of filtered) {
      if (token?.isCancellationRequested) break;
      out.push(loc);
      batch.push(loc);
      if (batch.length >= CHUNK) {
        try { (connection as any).sendProgress('$/progress', params.partialResultToken, { kind: 'report', message: `references: +${batch.length}`, items: batch }); } catch {}
        _reportProgress(params.workDoneToken, `references: +${batch.length}`);
        try { if (settings.streaming?.logChunks) connection.console.log(`references chunk: +${batch.length}`); } catch {}
        batch = [];
      }
    }
    if (batch.length > 0) {
      try { (connection as any).sendProgress('$/progress', params.partialResultToken, { kind: 'report', message: `references: +${batch.length}`, items: batch }); } catch {}
      _reportProgress(params.workDoneToken, `references: +${batch.length}`);
      try { if (settings.streaming?.logChunks) connection.console.log(`references chunk: +${batch.length}`); } catch {}
    }
    _endProgress(params.workDoneToken);
    return out;
  });

  // onRenameRequest: 重命名符号
  connection.onRenameRequest(async (params: RenameParams & ParamsWithProgress, token?: any): Promise<WorkspaceEdit | null> => {
    const doc = documents.get(params.textDocument.uri);
    if (!doc) return null;
    const text = doc.getText();
    const offset = doc.offsetAt(params.position);
    const word = captureWordAt(text, offset);
    if (!word) return null;
    const changes: Record<string, import('vscode-languageserver/node.js').TextEdit[]> = {};
    const settings = await getDocumentSettings(doc.uri).catch(() => defaultSettings);
    const scope = settings.rename?.scope ?? 'workspace';
    const openUris = new Set(documents.keys());
    let processed = 0;
    const modules = getAllModules();
    const total = modules.length;
    try { await updateDocumentIndex(doc.uri, doc.getText()); } catch {}
    _beginProgress(params.workDoneToken, 'Aster rename');
    const CHUNK = settings.streaming?.renameChunk ?? 200;
    const BATCH_SIZE = 20; // 批量并发读取文件数
    let editsInChunk = 0;

    // 批量异步处理模块
    for (let i = 0; i < modules.length; i += BATCH_SIZE) {
      if (token?.isCancellationRequested) break;

      const batch = modules.slice(i, i + BATCH_SIZE);
      const batchResults = await Promise.all(
        batch.map(async (rec) => {
          if (scope === 'open' && !openUris.has(rec.uri)) return null;
          try {
            const uri = ensureUri(rec.uri);
            const fsPath = uriToFsPath(rec.uri) || rec.uri;
            const t = await fsPromises.readFile(fsPath, 'utf8');
            const positions = findTokenPositionsSafe(t, word);
            if (positions.length === 0) return null;
            const edits: import('vscode-languageserver/node.js').TextEdit[] = positions.map(p => ({
              range: { start: offsetToPos(t, p.start), end: offsetToPos(t, p.end) },
              newText: params.newName
            }));
            return { uri, edits };
          } catch {
            return null;
          }
        })
      );

      // 合并结果
      for (const result of batchResults) {
        if (!result) continue;
        changes[result.uri] = (changes[result.uri] || []).concat(result.edits);
        editsInChunk += result.edits.length;
        if (editsInChunk >= CHUNK) {
          _reportProgress(params.workDoneToken, `rename: +${editsInChunk}`);
          editsInChunk = 0;
        }
      }

      processed += batch.length;
      if (processed % 50 === 0 || processed === total) {
        _reportProgress(params.workDoneToken, `${processed}/${total}`);
      }
    }
    _endProgress(params.workDoneToken);
    return { changes };
  });

  // onHover: 悬停提示
  connection.onHover(async params => {
    const doc = documents.get(params.textDocument.uri);
    if (!doc) return null;
    const entry = getOrParse(doc);
    const { tokens: toks, ast } = entry;
    const pos = params.position;

    // Build token index once for O(log n) lookups
    const tokenIndex = buildTokenIndex(toks);

    const anyCall = findDottedCallRangeAt(toks, pos);
    if (anyCall) {
      // Include a short signature preview by heuristic
      const info = describeDottedCallAt(toks, pos);
      const sig = info ? `${info.name}(${info.argDescs.join(', ')})` : 'interop(...)';
      const diags = findAmbiguousInteropCalls(toks);
      const amb = diags.find(
        d =>
          d.range.start.line <= pos.line &&
          d.range.end.line >= pos.line &&
          d.range.start.character <= pos.character &&
          d.range.end.character >= pos.character
      );
      const header = amb ? 'Ambiguous interop call' : 'Interop call';
      const body = amb
        ? 'Use `1L` or `1.0` to disambiguate; a Quick Fix is available.'
        : 'Overload selection uses primitive widening/boxing; use `1L` or `1.0` to make intent explicit.';
      const desc = info ? buildDescriptorPreview(info.name, info.argDescs) : null;
      const retText = returnTypeTextFromDesc(desc);
      const extra = desc ? `\nDescriptor: \`${desc}\`` : '';
      const retLine = retText ? `\nReturns: **${retText}**` : '';
      const msg = `**${header}** — [Guide → JVM Interop Overloads](/guide/interop-overloads)\n\nPreview: \`${sig}\`${extra}${retLine}\n\n${body}`;
      return { contents: { kind: 'markdown', value: msg } };
    }
    // Semantic hover: decls/params/locals/types using AST spans and tokens
    try {
      const ast2 = (ast as AstModule) || (parse(toks) as AstModule);
      const decl = findDeclAt(ast2, pos);
      if (decl) {
        if (isAstFunc(decl)) {
          const f = decl;
          // Use optimized O(log n) token lookup
          const nameAt = tokenNameAtOptimized(tokenIndex, pos);
          // Pattern bindings: if hover is inside a case body and name matches a binding
          const patInfo = nameAt ? findPatternBindingDetail(f, nameAt, pos) : null;
          if (patInfo) {
            const ofTxt = patInfo.ofType ? ` of ${patInfo.ofType}` : '';
            return { contents: { kind: 'markdown', value: `Pattern binding ${patInfo.name}${ofTxt}` } };
          }
          if (nameAt) {
            const param = f.params.find(p => p.name === nameAt);
            if (param) return { contents: { kind: 'markdown', value: `Parameter ${param.name}: ${typeText(param.type)}` } };
            const localInfo = findLocalLetWithExpr(f.body as AstBlock | null, nameAt);
            if (localInfo) {
              const hint = exprTypeText(localInfo.expr);
              return { contents: { kind: 'markdown', value: `Local ${nameAt}${hint ? ': ' + hint : ''}` } };
            }
          }
          return { contents: { kind: 'markdown', value: `Function ${f.name} — ${funcDetail(f)}` } };
        }
        if (isAstData(decl)) {
          const d = decl;
          const fields = d.fields.map(f => `${f.name}: ${typeText(f.type)}`).join(', ');
          return { contents: { kind: 'markdown', value: `type ${d.name}${fields ? ' — ' + fields : ''}` } };
        }
        if (isAstEnum(decl)) {
          const e = decl;
          return { contents: { kind: 'markdown', value: `enum ${e.name} — ${e.variants.join(', ')}` } };
        }
      }
    } catch {
      // ignore
    }
    return null;
  });

  // collectBlockSymbols 辅助函数（内部使用）
  const collectBlockSymbols = (b: AstBlock, parent: DocumentSymbol, doc: TextDocument): void => {
    const statements = getStatements(b);
    for (const s of statements) {
      if (s.kind === 'Let') {
        const letS = s;
        const sp = getSpan(letS);
        parent.children!.push({
          name: letS.name,
          kind: SymbolKind.Variable,
          range: spanOrDoc(sp, doc),
          selectionRange: spanOrDoc(sp, doc),
        });
      } else if (isAstBlock(s)) {
        const sp = getSpan(s);
        const bs: DocumentSymbol = {
          name: 'block',
          kind: SymbolKind.Namespace,
          range: spanOrDoc(sp, doc),
          selectionRange: spanOrDoc(sp, doc),
          children: [],
        };
        collectBlockSymbols(s, bs, doc);
        parent.children!.push(bs);
      } else if (s.kind === 'If') {
        // Collect nested blocks
        const thenB = s.thenBlock as AstBlock;
        const sp = getSpan(s);
        const thenS: DocumentSymbol = {
          name: 'if',
          kind: SymbolKind.Namespace,
          range: spanOrDoc(sp, doc),
          selectionRange: spanOrDoc(sp, doc),
          children: [],
        };
        collectBlockSymbols(thenB, thenS, doc);
        if (s.elseBlock) collectBlockSymbols(s.elseBlock as AstBlock, thenS, doc);
        parent.children!.push(thenS);
      } else if (s.kind === 'Match') {
        const sp = getSpan(s);
        const ms: DocumentSymbol = {
          name: 'match',
          kind: SymbolKind.Namespace,
          range: spanOrDoc(sp, doc),
          selectionRange: spanOrDoc(sp, doc),
          children: [],
        };
        for (const c of s.cases) {
          if (c.body.kind === 'Block') collectBlockSymbols(c.body as AstBlock, ms, doc);
        }
        parent.children!.push(ms);
      }
    }
  };

  // onDocumentSymbol: 文档符号树
  connection.onDocumentSymbol((params: DocumentSymbolParams) => {
    const doc = documents.get(params.textDocument.uri);
    if (!doc) return [];
    const entry = getOrParse(doc);
    const { tokens: toks, ast } = entry;
    try {
      const ast2 = (ast as AstModule) || (parse(toks) as AstModule);
      const symbols: DocumentSymbol[] = [];

      // Module symbol (if named)
      if (ast2.name) {
        symbols.push({
          name: ast2.name,
          kind: SymbolKind.Module,
          range: spanOrDoc(ast2.span, doc),
          selectionRange: spanOrDoc(ast2.span, doc),
          children: [],
        });
      }

      const pushChild = (parent: DocumentSymbol | null, sym: DocumentSymbol): void => {
        if (parent) {
          (parent.children ??= []).push(sym);
        } else {
          symbols.push(sym);
        }
      };

      // Top-level decls
      const moduleParent = symbols.find(s => s.kind === SymbolKind.Module) ?? null;
      for (const d of ast2.decls as AstDecl[]) {
        switch (d.kind) {
          case 'Data': {
            const data = d as AstData;
            const sp = getSpan(data);
            const ds: DocumentSymbol = {
              name: data.name,
              kind: SymbolKind.Struct,
              range: spanOrDoc(sp, doc),
              selectionRange: spanOrDoc(sp, doc),
              children: [],
              detail: 'type',
            };
            // fields
            for (const f of data.fields) {
              const fsp = getSpan(f);
              ds.children!.push({
                name: f.name,
                kind: SymbolKind.Field,
                range: spanOrDoc(fsp, doc),
                selectionRange: spanOrDoc(fsp, doc),
                detail: typeText(f.type),
              });
            }
            pushChild(moduleParent, ds);
            break;
          }
          case 'Enum': {
            const en = d as AstEnum;
            const sp = getSpan(en);
            const es: DocumentSymbol = {
              name: en.name,
              kind: SymbolKind.Enum,
              range: spanOrDoc(sp, doc),
              selectionRange: spanOrDoc(sp, doc),
              children: [],
            };
            const vspans: (Span | undefined)[] = getVariantSpans(en);
            for (let vi = 0; vi < en.variants.length; vi++) {
              const v = en.variants[vi]!;
              const vsp = vspans[vi];
              es.children!.push({ name: v, kind: SymbolKind.EnumMember, range: spanOrDoc(vsp, doc), selectionRange: spanOrDoc(vsp, doc) });
            }
            pushChild(moduleParent, es);
            break;
          }
          case 'Func': {
            const f = d as AstFunc;
            const sp = getSpan(f);
            const nsp = getNameSpan(f) ?? sp;
            const fs: DocumentSymbol = {
              name: f.name,
              kind: SymbolKind.Function,
              range: spanOrDoc(sp, doc),
              selectionRange: spanOrDoc(nsp, doc),
              children: [],
              detail: funcDetail(f),
            };
            // params
            for (const p of f.params) {
              const psp = getSpan(p);
              fs.children!.push({
                name: p.name,
                kind: SymbolKind.Variable,
                range: spanOrDoc(psp, doc),
                selectionRange: spanOrDoc(psp, doc),
                detail: typeText(p.type),
              });
            }
            // locals from body
            if (f.body) collectBlockSymbols(f.body, fs, doc);
            pushChild(moduleParent, fs);
            break;
          }
          default:
            break;
        }
      }
      return symbols;
    } catch {
      return [];
    }
  });

  // onDefinition: 跳转到定义
  connection.onDefinition(params => {
    const doc = documents.get(params.textDocument.uri);
    if (!doc) return null;
    const { tokens: toks, ast } = getOrParse(doc);

    // Build token index once for O(log n) lookups
    const tokenIndex = buildTokenIndex(toks);

    try {
      const ast2 = (ast as AstModule) || (parse(toks) as AstModule);
      // Use optimized O(log n) token lookup
      const name = tokenNameAtOptimized(tokenIndex, params.position);
      if (!name) return null;

      // Index top-level decls
      const declMap = new Map<string, Span | undefined>();
      for (const d of ast2.decls as AstDecl[]) {
        if (d.kind === 'Func' || d.kind === 'Data' || d.kind === 'Enum') {
          // Prefer function nameSpan when present
          const nm = (d as { name: string }).name;
          const nsp = getNameSpan(d);
          const sp = getSpan(d);
          declMap.set(nm, nsp ?? sp);
        }
      }
      if (declMap.has(name)) {
        const sp = declMap.get(name);
        if (sp) return toLocation(doc.uri, sp);
      }

      // Enum variant definitions
      const vmap = enumVariantSpanMap(ast2);
      if (vmap.has(name)) return toLocation(doc.uri, vmap.get(name)!);

      // Data field definitions when hovering a field initializer
      const cf = findConstructFieldAt(ast2, params.position);
      if (cf && cf.field === name) {
        const fmap = dataFieldSpanMap(ast2);
        const key = `${cf.typeName}.${cf.field}`;
        if (fmap.has(key)) return toLocation(doc.uri, fmap.get(key)!);
      }

      // Cross-file: dotted references Module.name resolve against open-docs index
      const dot = name.lastIndexOf('.');
      if (dot > 0) {
        const mod = name.substring(0, dot);
        const mem = name.substring(dot + 1);
        const rec = getAllModules().find(m => m.moduleName === mod);
        if (rec) {
          const symbol = rec.symbols.find(s => s.name === mem);
          const range = symbol?.selectionRange ?? symbol?.range;
          if (range) return { uri: ensureUri(rec.uri), range };
        }
      }

      // If inside a function, check params and lets
      const here = findDeclAt(ast2, params.position);
      if (here && isAstFunc(here)) {
        const f = here;
        // params
        const pHit = f.params.find(p => p.name === name);
        if (pHit) {
          const psp = getSpan(pHit) || getSpan(f);
          if (psp) return toLocation(doc.uri, psp);
        }
        // lets
        const lets = collectLetsWithSpan(f.body as AstBlock | null);
        if (lets.has(name)) {
          const sp = lets.get(name)!;
          return toLocation(doc.uri, sp);
        }
      }
    } catch {
      // ignore
    }
    return null;
  });

  // onPrepareRename: 预先校验是否可重命名并返回精确范围
  connection.onPrepareRename(params => {
    const doc = documents.get(params.textDocument.uri);
    if (!doc) return null;

    const entry = getOrParse(doc);
    const { tokens: toks } = entry;
    const nameAt = tokenNameAt(toks, params.position);
    if (!nameAt) {
      // 光标未命中有效标识符，直接拒绝
      return null;
    }

    const text = doc.getText();
    const offset = doc.offsetAt(params.position);
    const precisePositions = findTokenPositionsSafe(text, nameAt);
    const precise = precisePositions.find(pos => offset >= pos.start && offset <= pos.end);
    if (precise) {
      return {
        range: {
          start: offsetToPos(text, precise.start),
          end: offsetToPos(text, precise.end),
        },
        placeholder: text.slice(precise.start, precise.end),
      };
    }

    // 词法精确匹配失败时回退到简单的词边界捕获
    const fallback = captureWordAt(text, offset);
    if (!fallback) return null;
    const isWord = (c: string): boolean => /[A-Za-z0-9_.]/.test(c);
    let start = offset;
    while (start > 0 && isWord(text[start - 1]!)) start--;
    let end = offset;
    while (end < text.length && isWord(text[end]!)) end++;

    return {
      range: {
        start: offsetToPos(text, start),
        end: offsetToPos(text, end),
      },
      placeholder: text.slice(start, end),
    };
  });
}
