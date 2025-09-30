#!/usr/bin/env node

// Basic LSP server foundation for Aster CNL

import {
  createConnection,
  TextDocuments,
  Diagnostic,
  DiagnosticSeverity,
  ProposedFeatures,
  InitializeParams,
  DidChangeConfigurationNotification,
  DidChangeWatchedFilesNotification,
  CompletionItem,
  CompletionItemKind,
  TextDocumentSyncKind,
  InitializeResult,
  CodeActionKind,
  type CodeAction,
  type CodeActionParams,
  TextEdit,
  type Range,
  Location,
  type ReferenceParams,
  type RenameParams,
  type WorkspaceEdit,
  SemanticTokens,
  type SemanticTokensParams,
  type SemanticTokensLegend,
  type WorkspaceSymbol,
  // SemanticTokenTypes is a const enum of strings; not typed here
  DocumentDiagnosticReportKind,
  type DocumentDiagnosticReport,
  DocumentDiagnosticRequest,
  WorkspaceDiagnosticRequest,
  type WorkspaceDiagnosticReport,
  type WorkspaceDocumentDiagnosticReport,
  type InlayHint,
  InlayHintKind,
  type InlayHintParams,
  SymbolKind,
  type DocumentSymbol,
  type DocumentSymbolParams,
} from 'vscode-languageserver/node.js';

import { typecheckModule, typecheckModuleWithCapabilities, type TypecheckDiagnostic } from '../typecheck.js';
import fs from 'node:fs';
import type { CapabilityManifest } from '../capabilities.js';

import { TextDocument } from 'vscode-languageserver-textdocument';

import { canonicalize } from '../canonicalizer.js';
import { lex } from '../lexer.js';
// (no TokenKind import; analysis operates on tokens directly)
import { findAmbiguousInteropCalls, computeDisambiguationEdits, findDottedCallRangeAt, describeDottedCallAt, buildDescriptorPreview, returnTypeTextFromDesc } from './analysis.js';
import { parse } from '../parser.js';
import { TokenKind } from '../tokens.js';
import type {
  Module as AstModule,
  Declaration as AstDecl,
  Func as AstFunc,
  Data as AstData,
  Enum as AstEnum,
  Block as AstBlock,
  Statement as AstStmt,
  Let as AstLet,
  Type as AstType,
  Span,
} from '../types.js';
import { DiagnosticError } from '../diagnostics.js';
import { KW } from '../tokens.js';
import { buildCstLossless } from '../cst_builder.js';
import { printRangeFromCst } from '../cst_printer.js';
import { buildIdIndex, exprTypeText } from './utils.js';
// import { lowerModule } from "../lower_to_core";

// Create a connection for the server, using Node's IPC as a transport.
const connection = createConnection(ProposedFeatures.all);

// Create a simple text document manager.
const documents: TextDocuments<TextDocument> = new TextDocuments(TextDocument);
type IndexedDecl = { name: string; kind: 'Func' | 'Data' | 'Enum'; span?: Span; nameSpan?: Span };
type IndexedDoc = { uri: string; moduleName: string | null; decls: IndexedDecl[] };
const indexByUri: Map<string, IndexedDoc> = new Map();
const indexByModule: Map<string, IndexedDoc> = new Map();

type CachedDoc = {
  version: number;
  text: string;
  can: string;
  tokens: readonly any[];
  ast: AstModule | null;
  idIndex?: Map<string, Span[]>;
};
const docCache: Map<string, CachedDoc> = new Map();
const pendingValidate: Map<string, ReturnType<typeof setTimeout>> = new Map();
let indexWriteTimer: ReturnType<typeof setTimeout> | null = null;

function getOrParse(doc: TextDocument): CachedDoc {
  const key = doc.uri;
  const prev = docCache.get(key);
  if (prev && prev.version === doc.version) return prev;
  const text = doc.getText();
  const can = canonicalize(text);
  const tokens = lex(can);
  let ast: AstModule | null = null;
  try {
    ast = parse(tokens) as AstModule;
  } catch {
    ast = null;
  }
  const entry: CachedDoc = { version: doc.version, text, can, tokens, ast };
  // Build simple identifier index for performance (by token value)
  entry.idIndex = buildIdIndex(tokens);
  docCache.set(key, entry);
  // Update simple index for open docs
  try {
    const modName = entry.ast?.name ?? extractModuleName(entry.text);
    const decls: IndexedDecl[] = [];
    if (entry.ast) {
      for (const d of entry.ast.decls as AstDecl[]) {
        if (d.kind === 'Func' || d.kind === 'Data' || d.kind === 'Enum')
          decls.push({ name: (d as any).name, kind: d.kind as any, span: (d as any).span, nameSpan: (d as any).nameSpan });
      }
    }
    const rec: IndexedDoc = { uri: doc.uri, moduleName: modName, decls };
    indexByUri.set(doc.uri, rec);
    if (modName) indexByModule.set(modName, rec);
  } catch {
    // ignore
  }
  return entry;
}

let hasConfigurationCapability = false;
let hasWorkspaceFolderCapability = false;
let hasDiagnosticRelatedInformationCapability = false;
let indexPersistEnabled = true;
let indexPathOverride: string | null = null;
let hasWatchedFilesCapability = false;
let watcherRegistered = false;
const HEALTH_METHOD = 'aster/health';
let workspaceDiagnosticsEnabled = true;

connection.onInitialize((params: InitializeParams) => {
  const capabilities = params.capabilities;

  // Does the client support the `workspace/configuration` request?
  hasConfigurationCapability = !!(capabilities.workspace && !!capabilities.workspace.configuration);
  hasWorkspaceFolderCapability = !!(
    capabilities.workspace && !!capabilities.workspace.workspaceFolders
  );
  hasDiagnosticRelatedInformationCapability = !!(
    capabilities.textDocument &&
    capabilities.textDocument.publishDiagnostics &&
    capabilities.textDocument.publishDiagnostics.relatedInformation
  );
  hasWatchedFilesCapability = !!(
    capabilities.workspace &&
    (capabilities.workspace as any).didChangeWatchedFiles &&
    (capabilities.workspace as any).didChangeWatchedFiles.dynamicRegistration
  );

  const result: InitializeResult = {
    capabilities: {
      textDocumentSync: TextDocumentSyncKind.Incremental,
      // Tell the client that this server supports code completion.
      completionProvider: {
        resolveProvider: true,
        triggerCharacters: [' ', '.', ':'],
      },
      diagnosticProvider: {
        interFileDependencies: false,
        workspaceDiagnostics: true,
      },
      codeActionProvider: {
        codeActionKinds: [CodeActionKind.QuickFix],
      },
      hoverProvider: true,
      documentSymbolProvider: true,
      semanticTokensProvider: {
        legend: SEM_LEGEND,
        range: false,
        full: true,
      },
      workspaceSymbolProvider: true,
      documentFormattingProvider: true,
      documentRangeFormattingProvider: true,
      inlayHintProvider: true,
    },
  };
  if (hasWorkspaceFolderCapability) {
    result.capabilities.workspace = {
      workspaceFolders: {
        supported: true,
      },
    };
  }
  return result;
});

connection.onInitialized(() => {
  if (hasConfigurationCapability) {
    // Register for all configuration changes.
    connection.client.register(DidChangeConfigurationNotification.type, undefined);
  }
  if (hasWorkspaceFolderCapability) {
    connection.workspace.onDidChangeWorkspaceFolders(() => {
      connection.console.log('Workspace folder change event received.');
    });
  }
  tryLoadPersistedIndex();
  // Respond to external file changes if client supports it
  if (hasWatchedFilesCapability) {
    try {
      connection.client.register(DidChangeWatchedFilesNotification.type, {
        watchers: [{ globPattern: '**/*.cnl' }],
      });
      watcherRegistered = true;
    } catch {
      // ignore registration failure
    }
    connection.onDidChangeWatchedFiles(ev => {
      try {
        for (const ch of ev.changes) {
          const path = uriToFsPath(ch.uri);
          if (!path) continue;
          if (fs.existsSync(path)) {
            const text = fs.readFileSync(path, 'utf8');
            const doc = TextDocument.create(ch.uri, 'cnl', 0, text);
            updateIndexForDocument(doc);
          } else {
            indexByUri.delete(ch.uri);
          }
        }
        scheduleIndexWrite();
      } catch {
        // ignore
      }
    });
  } else {
    connection.console.warn('Client does not advertise didChangeWatchedFiles; workspace index updates may be limited to open documents.');
  }
  // Health request handler
  connection.onRequest(HEALTH_METHOD, () => {
    return {
      watchers: {
        capability: hasWatchedFilesCapability,
        registered: watcherRegistered,
      },
      index: {
        files: indexByUri.size,
        modules: indexByModule.size,
      },
    } as const;
  });
});

// The example settings
interface AsterSettings {
  maxNumberOfProblems: number;
  format?: { mode?: 'lossless' | 'normalize'; reflow?: boolean };
  index?: { persist?: boolean; path?: string };
  rename?: { scope?: 'open' | 'workspace' };
  diagnostics?: { workspace?: boolean };
}

// The global settings, used when the `workspace/configuration` request is not supported by the client.
const defaultSettings: AsterSettings = { maxNumberOfProblems: 1000, format: { mode: 'lossless', reflow: true }, index: { persist: true }, rename: { scope: 'workspace' }, diagnostics: { workspace: true } };
let globalSettings: AsterSettings = defaultSettings;

// Cache the settings of all open documents
const documentSettings: Map<string, Promise<AsterSettings>> = new Map();

connection.onDidChangeConfiguration(change => {
  if (hasConfigurationCapability) {
    // Reset all cached document settings
    documentSettings.clear();
  } else {
    globalSettings = <AsterSettings>(change.settings.asterLanguageServer || defaultSettings);
  }

  // Revalidate caches for open documents (pull diagnostics will request when needed)
  documents.all().forEach(doc => { try { void getOrParse(doc); } catch {} });
  // Update index settings
  getDocumentSettings('').then(s => {
    indexPersistEnabled = s.index?.persist ?? true;
    indexPathOverride = s.index?.path ?? null;
    workspaceDiagnosticsEnabled = s.diagnostics?.workspace ?? true;
  }).catch(() => {
    indexPersistEnabled = true;
    indexPathOverride = null;
    workspaceDiagnosticsEnabled = true;
  });
});

function getDocumentSettings(resource: string): Promise<AsterSettings> {
  if (!hasConfigurationCapability) {
    return Promise.resolve(globalSettings);
  }
  let result = documentSettings.get(resource);
  if (!result) {
    result = connection.workspace.getConfiguration({
      scopeUri: resource,
      section: 'asterLanguageServer',
    });
    documentSettings.set(resource, result);
  }
  return result;
}

// Only keep settings for open documents
documents.onDidClose(e => {
  documentSettings.delete(e.document.uri);
});

// The content of a text document has changed. This event is emitted
// when the text document first opened or when its content has changed.
documents.onDidChangeContent(change => {
  const uri = change.document.uri;
  const prev = pendingValidate.get(uri);
  if (prev) clearTimeout(prev);
  const handle = setTimeout(() => {
    pendingValidate.delete(uri);
    // Diagnostics are now served via pull (textDocument/diagnostic).
    // We still parse to keep caches warm for fast responses.
    try { void getOrParse(change.document); } catch {}
  }, 150);
  pendingValidate.set(uri, handle);
  // Update index for open document
  try {
    updateIndexForDocument(change.document);
    scheduleIndexWrite();
  } catch {
    // ignore
  }
});

documents.onDidSave(e => {
  try {
    updateIndexForDocument(e.document);
    scheduleIndexWrite();
  } catch {
    // ignore
  }
});

// Range formatting provider (lossless with minimal seam reflow)
connection.onDocumentRangeFormatting(async (params) => {
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return [];
  try {
    const settings = await getDocumentSettings(doc.uri);
    const mode = settings.format?.mode ?? 'lossless';
    const reflow = !!settings.format?.reflow;
    const text = doc.getText();
    const start = doc.offsetAt(params.range.start);
    const end = doc.offsetAt(params.range.end);
    let out: string;
    if (mode === 'lossless') {
      const cst = buildCstLossless(text);
      out = printRangeFromCst(cst, start, end, { reflow });
    } else {
      // Normalize mode: format the slice via strict formatter
      const { formatCNL } = await import('../formatter.js');
      const slice = text.slice(start, end);
      out = formatCNL(slice, { mode: 'normalize' });
    }
    const edit: TextEdit = { range: params.range, newText: out };
    return [edit];
  } catch {
    return [];
  }
});

// Full document formatting provider
connection.onDocumentFormatting(async (params) => {
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return [];
  try {
    const settings = await getDocumentSettings(doc.uri);
    const mode = settings.format?.mode ?? 'lossless';
    const reflow = !!settings.format?.reflow;
    const text = doc.getText();
    let out: string;
    if (mode === 'lossless') {
      const { buildCstLossless } = await import('../cst_builder.js');
      const { printCNLFromCst } = await import('../cst_printer.js');
      const cst = buildCstLossless(text);
      out = printCNLFromCst(cst, { reflow });
    } else {
      const { formatCNL } = await import('../formatter.js');
      out = formatCNL(text, { mode: 'normalize' });
    }
    const fullRange: Range = {
      start: { line: 0, character: 0 },
      end: doc.positionAt(text.length),
    };
    const edit: TextEdit = { range: fullRange, newText: out };
    return [edit];
  } catch {
    return [];
  }
});
documents.onDidClose(e => {
  docCache.delete(e.document.uri);
  const rec = indexByUri.get(e.document.uri);
  if (rec) {
    indexByUri.delete(e.document.uri);
    if (rec.moduleName) indexByModule.delete(rec.moduleName);
  }
});

function updateIndexForDocument(doc: TextDocument): void {
  const text = doc.getText();
  const can = canonicalize(text);
  const tokens = lex(can);
  let ast: AstModule | null = null;
  try {
    ast = parse(tokens) as AstModule;
  } catch {
    ast = null;
  }
  if (!ast) return;
  const decls: IndexedDecl[] = [];
  for (const d of ast.decls as AstDecl[]) {
    if (d.kind === 'Func' || d.kind === 'Data' || d.kind === 'Enum')
      decls.push({ name: (d as any).name, kind: d.kind as any, span: (d as any).span, nameSpan: (d as any).nameSpan });
  }
  const rec: IndexedDoc = { uri: doc.uri, moduleName: ast.name ?? null, decls };
  indexByUri.set(doc.uri, rec);
  if (rec.moduleName) indexByModule.set(rec.moduleName, rec);
}

function scheduleIndexWrite(): void {
  if (!indexPersistEnabled) return;
  if (indexWriteTimer) clearTimeout(indexWriteTimer);
  indexWriteTimer = setTimeout(() => {
    try {
      const root = process.cwd();
      const outPath = indexPathOverride || pathJoin(root, '.asteri', 'lsp-index.json');
      const fs = require('node:fs');
      const path = require('node:path');
      fs.mkdirSync(path.dirname(outPath), { recursive: true });
      const files = Array.from(indexByUri.values());
      const payload = { version: 1, generatedAt: new Date().toISOString(), root, files };
      fs.writeFileSync(outPath, JSON.stringify(payload, null, 2), 'utf8');
      connection.console.log(`Index written: ${path.relative(root, outPath)}`);
    } catch (e: any) {
      connection.console.warn(`Failed writing index: ${e?.message ?? String(e)}`);
    }
  }, 500);
}

function pathJoin(...parts: string[]): string {
  return require('node:path').join.apply(null, parts as any);
}

// Workspace symbols across open documents
connection.onWorkspaceSymbol(({ query }): WorkspaceSymbol[] => {
  const out: WorkspaceSymbol[] = [];
  const q = (query || '').toLowerCase();
  for (const rec of indexByUri.values()) {
    for (const d of rec.decls) {
      if (q && !d.name.toLowerCase().includes(q)) continue;
      const sp = d.nameSpan || d.span;
      if (!sp) continue;
      out.push({
        name: rec.moduleName ? `${rec.moduleName}.${d.name}` : d.name,
        kind: d.kind === 'Func' ? SymbolKind.Function : d.kind === 'Data' ? SymbolKind.Struct : SymbolKind.Enum,
        location: toLocation(rec.uri, sp),
      });
    }
  }
  return out;
});

// Document links: module header and dotted Module.member → target module file (if known),
// and common guide links (e.g., Text.* → interop overloads guide)
connection.onDocumentLinks(params => {
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return [];
  const text = doc.getText();
  const out: any[] = [];
  try {
    const m = text.match(/This module is ([A-Za-z][A-Za-z0-9_.]*)\./);
    if (m) {
      const mod = m[1]!;
      const rec = indexByModule.get(mod);
      if (rec && ensureUri(rec.uri) !== ensureUri(doc.uri)) {
        const startOff = (m.index ?? 0) + 'This module is '.length;
        const endOff = startOff + mod.length;
        out.push({ range: { start: offsetToPos(text, startOff), end: offsetToPos(text, endOff) }, target: ensureUri(rec.uri) });
      }
    }
    const dotted = /\b([A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+)\b/g;
    for (let mm; (mm = dotted.exec(text)); ) {
      const full = mm[1]!;
      const dot = full.lastIndexOf('.');
      const mod = full.slice(0, dot);
      const rec = indexByModule.get(mod);
      if (!rec) continue;
      const s = mm.index!;
      out.push({ range: { start: offsetToPos(text, s), end: offsetToPos(text, s + mod.length) }, target: ensureUri(rec.uri) });
    }
    // Guide link for Text.* helpers: link the 'Text' part to interop-overloads guide
    const guide = toGuideUri('docs/guide/interop-overloads.md');
    if (guide) {
      const reText = /\bText\s*\./g;
      for (let m2; (m2 = reText.exec(text)); ) {
        const s = m2.index!;
        out.push({ range: { start: offsetToPos(text, s), end: offsetToPos(text, s + 4) }, target: guide });
      }
    }
  } catch {
    // ignore
  }
  return out;
});

// Cross-file references
connection.onReferences(async (params: ReferenceParams, token?: any) => {
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
  let processed = 0;
  const total = indexByUri.size;
  // Progress: begin
  beginProgress((params as any).workDoneToken, 'Aster references');
  for (const rec of indexByUri.values()) {
    if (scope === 'open' && !openUris.has(rec.uri)) continue;
    if (token?.isCancellationRequested) break;
    try {
      const fsPath = uriToFsPath(rec.uri) || rec.uri;
      const t = fs.readFileSync(fsPath, 'utf8');
      const positions = findTokenPositionsSafe(t, word);
      for (const p of positions) out.push({ uri: ensureUri(rec.uri), range: { start: offsetToPos(t, p.start), end: offsetToPos(t, p.end) } });
    } catch {
      continue;
    }
    processed++;
    if (processed % 50 === 0) reportProgress((params as any).workDoneToken, `${processed}/${total}`);
  }
  // Progress: end
  endProgress((params as any).workDoneToken);
  return out;
});

connection.onRenameRequest(async (params: RenameParams, token?: any): Promise<WorkspaceEdit | null> => {
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return null;
  const text = doc.getText();
  const offset = doc.offsetAt(params.position);
  const word = captureWordAt(text, offset);
  if (!word) return null;
  const changes: Record<string, TextEdit[]> = {};
  const settings = await getDocumentSettings(doc.uri).catch(() => defaultSettings);
  const scope = settings.rename?.scope ?? 'workspace';
  const openUris = new Set(documents.keys());
  let processed = 0;
  const total = indexByUri.size;
  beginProgress((params as any).workDoneToken, 'Aster rename');
  for (const rec of indexByUri.values()) {
    if (scope === 'open' && !openUris.has(rec.uri)) continue;
    if (token?.isCancellationRequested) break;
    try {
      const uri = ensureUri(rec.uri);
      const fsPath = uriToFsPath(rec.uri) || rec.uri;
      const t = fs.readFileSync(fsPath, 'utf8');
      const positions = findTokenPositionsSafe(t, word);
      if (positions.length === 0) continue;
      const edits: TextEdit[] = positions.map(p => ({ range: { start: offsetToPos(t, p.start), end: offsetToPos(t, p.end) }, newText: params.newName }));
      changes[uri] = (changes[uri] || []).concat(edits);
    } catch {
      continue;
    }
    processed++;
    if (processed % 50 === 0) reportProgress((params as any).workDoneToken, `${processed}/${total}`);
  }
  endProgress((params as any).workDoneToken);
  return { changes };
});

function captureWordAt(text: string, offset: number): string | null {
  const isWord = (c: string): boolean => /[A-Za-z0-9_.]/.test(c);
  let s = offset;
  while (s > 0 && isWord(text[s - 1]!)) s--;
  let e = offset;
  while (e < text.length && isWord(text[e]!)) e++;
  if (s === e) return null;
  return text.slice(s, e);
}

function findWordPositions(text: string, word: string): Array<{ start: number; end: number }> {
  const out: Array<{ start: number; end: number }> = [];
  const re = new RegExp(`(?<![A-Za-z0-9_.])${word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(?![A-Za-z0-9_.])`, 'g');
  for (let m; (m = re.exec(text)); ) {
    out.push({ start: m.index, end: m.index + word.length });
  }
  return out;
}

function findTokenPositionsSafe(text: string, word: string): Array<{ start: number; end: number }> {
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

function buildLineStarts(text: string): number[] {
  const a: number[] = [0];
  for (let i = 0; i < text.length; i++) if (text[i] === '\n') a.push(i + 1);
  return a;
}
function toOffset(starts: readonly number[], line: number, col: number): number {
  const li = Math.max(1, line) - 1;
  const base = starts[li] ?? 0;
  return base + Math.max(1, col) - 1;
}

function offsetToPos(text: string, off: number): { line: number; character: number } {
  let line = 0;
  let last = 0;
  for (let i = 0; i < text.length && i < off; i++) if (text[i] === '\n') { line++; last = i + 1; }
  return { line, character: off - last };
}

function ensureUri(u: string): string {
  if (u.startsWith('file://')) return u;
  const path = require('node:path');
  const to = 'file://' + (path.isAbsolute(u) ? u : path.join(process.cwd(), u));
  return to;
}

function uriToFsPath(u: string): string | null {
  try {
    if (u.startsWith('file://')) return new URL(u).pathname;
  } catch {}
  return null;
}

// Work Done Progress helpers (no-op if token missing)
function beginProgress(token: unknown, title: string): void {
  try {
    if (!token) return;
    (connection as any).sendProgress({ method: '$/progress' }, token, { kind: 'begin', title });
  } catch {
    // ignore
  }
}
function reportProgress(token: unknown, message: string): void {
  try {
    if (!token) return;
    (connection as any).sendProgress({ method: '$/progress' }, token, { kind: 'report', message });
  } catch {
    // ignore
  }
}
function endProgress(token: unknown): void {
  try {
    if (!token) return;
    (connection as any).sendProgress({ method: '$/progress' }, token, { kind: 'end' });
  } catch {
    // ignore
  }
}

async function computeDiagnostics(textDocument: TextDocument): Promise<Diagnostic[]> {
  void (await getDocumentSettings(textDocument.uri));
  const { text, tokens, ast } = getOrParse(textDocument);
  const diagnostics: Diagnostic[] = [];

  try {
    const canonicalized = text; // text already cached
    void canonicalized;
    const toks = tokens;
    const parsed = ast ?? parse(tokens);
    // Lightweight lexical checks for interop
    diagnostics.push(...findAmbiguousInteropCalls(toks));
    diagnostics.push(...(await import('./analysis.js')).findNullabilityDiagnostics(toks));
    // Also run core + typecheck to surface semantic warnings
    try {
      const core = (await import('../lower_to_core.js')).lowerModule(parsed);
      // Load capability manifest if configured via env (singleton read)
      let manifest: CapabilityManifest | null = null;
      const capsPath = process.env.ASTER_CAPS || '';
      if (capsPath) {
        try { manifest = JSON.parse(fs.readFileSync(capsPath, 'utf8')); } catch { manifest = null; }
      }
      const tdiags: TypecheckDiagnostic[] = manifest
        ? typecheckModuleWithCapabilities(core, manifest)
        : typecheckModule(core);
      for (const td of tdiags) {
        const d: Diagnostic = {
          severity:
            td.severity === 'error' ? DiagnosticSeverity.Error : DiagnosticSeverity.Warning,
          range: {
            start: { line: 0, character: 0 },
            end: { line: 0, character: 0 },
          },
          message: td.message,
          source: 'aster-typecheck',
        };
        if (td.code !== null && td.code !== undefined) (d as any).code = td.code as string;
        if (td.data !== null && td.data !== undefined) (d as any).data = td.data as any;
        diagnostics.push(d);
      }
    } catch {
      // ignore typecheck failures here; parse errors handled below
    }
  } catch (error) {
    if (error instanceof DiagnosticError) {
      const diag = error.diagnostic;
      const diagnostic: Diagnostic = {
        severity:
          diag.severity === 'error'
            ? DiagnosticSeverity.Error
            : diag.severity === 'warning'
              ? DiagnosticSeverity.Warning
              : diag.severity === 'info'
                ? DiagnosticSeverity.Information
                : DiagnosticSeverity.Hint,
        range: {
          start: { line: diag.span.start.line - 1, character: diag.span.start.col - 1 },
          end: { line: diag.span.end.line - 1, character: diag.span.end.col - 1 },
        },
        message: diag.message,
        source: 'aster',
        code: diag.code,
      };

      if (diag.relatedInformation && hasDiagnosticRelatedInformationCapability) {
        diagnostic.relatedInformation = diag.relatedInformation.map(info => ({
          location: {
            uri: textDocument.uri,
            range: {
              start: { line: info.span.start.line - 1, character: info.span.start.col - 1 },
              end: { line: info.span.end.line - 1, character: info.span.end.col - 1 },
            },
          },
          message: info.message,
        }));
      }

      diagnostics.push(diagnostic);
    } else {
      // Fallback for non-diagnostic errors
      const diagnostic: Diagnostic = {
        severity: DiagnosticSeverity.Error,
        range: {
          start: { line: 0, character: 0 },
          end: { line: 0, character: 0 },
        },
        message: error instanceof Error ? error.message : String(error),
        source: 'aster',
      };
      diagnostics.push(diagnostic);
    }
  }

  try {
    // Add a gentle warning if the module header is missing; helps enable cross-file features
    const a = ast ?? parse(tokens);
    if (!a || !(a as any).name) {
      diagnostics.push({
        severity: DiagnosticSeverity.Warning,
        range: { start: { line: 0, character: 0 }, end: { line: 0, character: 0 } },
        message: 'Missing module header. Add "This module is <name>."',
        source: 'aster',
      });
    }
  } catch {
    // ignore
  }
  return diagnostics;
}

// LSP 3.17+ pull diagnostics handler
connection.onRequest(DocumentDiagnosticRequest.type, async (params): Promise<DocumentDiagnosticReport> => {
  try {
    const doc = documents.get(params.textDocument.uri);
    if (!doc) return { kind: DocumentDiagnosticReportKind.Full, items: [] };
    const items = await computeDiagnostics(doc);
    return { kind: DocumentDiagnosticReportKind.Full, items };
  } catch {
    return { kind: DocumentDiagnosticReportKind.Full, items: [] };
  }
});

// Workspace diagnostics (pull): compute diagnostics for all indexed files
connection.onRequest(WorkspaceDiagnosticRequest.type, async (): Promise<WorkspaceDiagnosticReport> => {
  if (!workspaceDiagnosticsEnabled) return { items: [] };
  const results: WorkspaceDocumentDiagnosticReport[] = [];
  try {
    for (const rec of indexByUri.values()) {
      try {
        const open = documents.get(rec.uri);
        if (open) {
          const items = await computeDiagnostics(open);
          results.push({ uri: ensureUri(rec.uri), version: open.version, kind: DocumentDiagnosticReportKind.Full, items });
        } else {
          const fsPath = uriToFsPath(rec.uri) || rec.uri;
          if (!fsPath || !fs.existsSync(fsPath)) continue;
          const text = fs.readFileSync(fsPath, 'utf8');
          const temp = TextDocument.create(ensureUri(rec.uri), 'cnl', 0, text);
          const items = await computeDiagnostics(temp);
          results.push({ uri: ensureUri(rec.uri), version: null, kind: DocumentDiagnosticReportKind.Full, items });
        }
      } catch {
        // skip file on error
      }
    }
  } catch {
    // ignore
  }
  return { items: results };
});

// Inlay hints: show literal types and simple let-inferred types
connection.onRequest('textDocument/inlayHint' as any, async (params: InlayHintParams): Promise<InlayHint[] | null> => {
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return null;
  const entry = getOrParse(doc);
  const { tokens: toks, ast } = entry;
  const out: InlayHint[] = [];
  const range = params.range;
  const within = (line: number, col: number): boolean => {
    const s = doc.offsetAt(range.start);
    const e = doc.offsetAt(range.end);
    const off = doc.offsetAt({ line, character: Math.max(0, col - 1) });
    return off >= s && off <= e;
  };
  try {
    for (const t of toks as any[]) {
      if (!t || !t.start) continue;
      const k = t.kind;
      let label: string | null = null;
      if (k === TokenKind.INT) label = 'Int';
      else if (k === TokenKind.LONG) label = 'Long';
      else if (k === TokenKind.FLOAT) label = 'Double';
      else if (k === TokenKind.STRING) label = 'Text';
      else if (k === TokenKind.BOOL) label = 'Bool';
      else if (k === TokenKind.NULL) label = 'null';
      if (label && within(t.start.line - 1, t.start.col)) {
        out.push({ position: { line: t.start.line - 1, character: t.start.col - 1 }, label, kind: InlayHintKind.Type });
      }
    }
    // Simple let type hints using exprTypeText
    const a = (ast as AstModule) || (parse(toks) as AstModule);
    for (const d of a.decls as AstDecl[]) {
      if (d.kind !== 'Func') continue;
      const f = d as AstFunc;
      // Function return type at function name
      const nsp = ((f as any).nameSpan as Span | undefined) || ((f as any).span as Span | undefined);
      try {
        const retTxt = typeText((f as any).retType);
        if (nsp && retTxt && within(nsp.start.line - 1, nsp.start.col)) {
          out.push({ position: { line: nsp.start.line - 1, character: Math.max(0, nsp.start.col - 1) }, label: ` -> ${retTxt}`, kind: InlayHintKind.Type });
        }
      } catch {}
      // Parameter type hints
      for (const p of f.params) {
        const psp = ((p as any).span as Span | undefined);
        try {
          const ptxt = typeText(p.type);
          if (psp && ptxt && within(psp.start.line - 1, psp.start.col)) {
            out.push({ position: { line: psp.start.line - 1, character: Math.max(0, psp.start.col - 1) }, label: `: ${ptxt}`, kind: InlayHintKind.Parameter });
          }
        } catch {}
      }
      const walk = (b: AstBlock): void => {
        for (const s of b.statements as AstStmt[]) {
          if (s.kind === 'Let') {
            const sp = (s as any).span as Span | undefined;
            const hint = exprTypeText((s as any).expr);
            if (sp && hint && within(sp.start.line - 1, sp.start.col)) {
              out.push({ position: { line: sp.start.line - 1, character: Math.max(0, sp.start.col - 1) }, label: `: ${hint}`, kind: InlayHintKind.Type });
            }
          } else if (s.kind === 'If') {
            walk(s.thenBlock as AstBlock);
            if (s.elseBlock) walk(s.elseBlock as AstBlock);
          } else if (s.kind === 'Match') {
            for (const c of s.cases) if (c.body.kind === 'Block') walk(c.body as AstBlock);
          } else if (s.kind === 'Block') walk(s as unknown as AstBlock);
        }
      };
      if (f.body) walk(f.body);
    }
  } catch {
    // ignore
  }
  return out;
});

connection.onCodeAction(async (params: CodeActionParams): Promise<CodeAction[]> => {
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return [];
  const text = doc.getText();
  const actions: CodeAction[] = [];
  const capsPath = process.env.ASTER_CAPS || '';
  for (const d of params.context.diagnostics) {
    const code = (d.code as string) || '';
    if (code === 'EFF_MISSING_IO' || code === 'EFF_MISSING_CPU') {
      const cap = code.endsWith('IO') ? 'IO' : 'CPU';
      const func = ((d as any).data?.func as string) || extractFuncNameFromMessage(d.message);
      const edit = headerInsertEffectEdit(text, func, cap);
      if (edit) actions.push({
        title: `Add It performs ${cap} to '${func}'`,
        kind: CodeActionKind.QuickFix,
        edit: { changes: { [params.textDocument.uri]: [edit] } },
        diagnostics: [d],
      });
    }
    if (code === 'EFF_SUPERFLUOUS_IO' || code === 'EFF_SUPERFLUOUS_CPU') {
      const cap = code.endsWith('IO') ? 'IO' : 'CPU';
      const func = ((d as any).data?.func as string) || extractFuncNameFromMessage(d.message);
      const edit = headerRemoveEffectEdit(text, func);
      if (edit) actions.push({
        title: `Remove It performs ${cap} from '${func}'`,
        kind: CodeActionKind.QuickFix,
        edit: { changes: { [params.textDocument.uri]: [edit] } },
        diagnostics: [d],
      });
    }
    if ((code === 'CAP_IO_NOT_ALLOWED' || code === 'CAP_CPU_NOT_ALLOWED') && capsPath) {
      const cap = code === 'CAP_IO_NOT_ALLOWED' ? 'io' : 'cpu';
      const func = ((d as any).data?.func as string) || extractFuncNameFromMessage(d.message);
      const mod = ((d as any).data?.module as string) || extractModuleName(text) || '';
      const fqn = mod ? `${mod}.${func}` : func;
      try {
        const fsText = fs.readFileSync(capsPath, 'utf8');
        const man = JSON.parse(fsText);
        const uri = toFileUri(capsPath);
        // Offer: allow for specific function (ensure FQN)
        {
          const manFn = structuredClone(man);
          const textFn = ensureCapabilityAllow(manFn, cap, fqn);
          actions.push({
            title: `Allow ${cap.toUpperCase()} for ${fqn} in manifest`,
            kind: CodeActionKind.QuickFix,
            edit: { changes: { [uri]: [TextEdit.replace(fullDocRange(), textFn)] } },
            diagnostics: [d],
          });
        }
        // Offer: allow for entire module (module.*)
        if (mod) {
          const modWildcard = `${mod}.*`;
          const manMod = structuredClone(man);
          const textMod = ensureCapabilityAllow(manMod, cap, modWildcard);
          actions.push({
            title: `Allow ${cap.toUpperCase()} for ${mod}.* in manifest`,
            kind: CodeActionKind.QuickFix,
            edit: { changes: { [uri]: [TextEdit.replace(fullDocRange(), textMod)] } },
            diagnostics: [d],
          });

          // If module.* is already present, offer to narrow to function-only (remove module.*)
          const arr: string[] = Array.isArray(man.allow?.[cap]) ? man.allow[cap] : [];
          if (arr.includes(modWildcard)) {
            const manNarrow = structuredClone(man);
            const textNarrow = swapCapabilityAllow(manNarrow, cap, modWildcard, fqn);
            actions.push({
              title: `Narrow ${cap.toUpperCase()} from ${mod}.* to ${fqn}`,
              kind: CodeActionKind.QuickFix,
              edit: { changes: { [uri]: [TextEdit.replace(fullDocRange(), textNarrow)] } },
              diagnostics: [d],
            });
          }
          // If function is already present, offer to broaden to module.* (remove fqn)
          if (arr.includes(fqn)) {
            const manBroad = structuredClone(man);
            const textBroad = swapCapabilityAllow(manBroad, cap, fqn, modWildcard);
            actions.push({
              title: `Broaden ${cap.toUpperCase()} from ${fqn} to ${mod}.*`,
              kind: CodeActionKind.QuickFix,
              edit: { changes: { [uri]: [TextEdit.replace(fullDocRange(), textBroad)] } },
              diagnostics: [d],
            });
          }
        }
      } catch {
        // ignore
      }
    }
  }
  return actions;
});

function fullDocRange(): Range {
  return { start: { line: 0, character: 0 }, end: { line: Number.MAX_SAFE_INTEGER, character: 0 } };
}

function extractFuncNameFromMessage(msg: string): string {
  const m = msg.match(/Function '([^']+)'/);
  return m?.[1] ?? '';
}

function extractModuleName(text: string): string | null {
  const m = text.match(/This module is ([A-Za-z][A-Za-z0-9_.]*)\./);
  return m?.[1] ?? null;
}

function headerInsertEffectEdit(text: string, func: string, cap: 'IO' | 'CPU'): TextEdit | null {
  const lines = text.split(/\r?\n/);
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i] ?? '';
    if (/^To\s+/i.test(line) && new RegExp(`\\b${func}\\b`).test(line)) {
      if (/It performs/i.test(line)) return null;
      const withEff = line.replace(/(:|\.)\s*$/, `. It performs ${cap}:`);
      return TextEdit.replace({ start: { line: i, character: 0 }, end: { line: i, character: line.length } }, withEff);
    }
  }
  return null;
}

function headerRemoveEffectEdit(text: string, func: string): TextEdit | null {
  const lines = text.split(/\r?\n/);
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i] ?? '';
    if (/^To\s+/i.test(line) && new RegExp(`\\b${func}\\b`).test(line) && /It performs/i.test(line)) {
      const cleaned = line.replace(/\. It performs (IO|CPU):/i, ':');
      return TextEdit.replace({ start: { line: i, character: 0 }, end: { line: i, character: line.length } }, cleaned);
    }
  }
  return null;
}

function ensureCapabilityAllow(man: any, cap: string, entry: string): string {
  const allow = (man.allow = man.allow || {});
  const arr: string[] = (allow[cap] = Array.isArray(allow[cap]) ? allow[cap] : []);
  if (!arr.includes(entry)) arr.push(entry);
  return JSON.stringify(man, null, 2) + '\n';
}

function swapCapabilityAllow(man: any, cap: string, removeEntry: string, addEntry: string): string {
  const allow = (man.allow = man.allow || {});
  const arr: string[] = (allow[cap] = Array.isArray(allow[cap]) ? allow[cap] : []);
  const idx = arr.indexOf(removeEntry);
  if (idx >= 0) arr.splice(idx, 1);
  if (!arr.includes(addEntry)) arr.push(addEntry);
  return JSON.stringify(man, null, 2) + '\n';
}

import path from 'node:path';
import { pathToFileURL } from 'node:url';
function toFileUri(p: string): string {
  const abs = path.isAbsolute(p) ? p : path.resolve(p);
  return String(pathToFileURL(abs));
}
function toGuideUri(rel: string): string | null {
  try {
    const p = require('node:path').join(process.cwd(), rel);
    if (require('node:fs').existsSync(p)) return toFileUri(p);
    return null;
  } catch { return null; }
}

// TODO: upgrade to LSP 3.17+ for document diagnostics when ready
// Currently not supported by vscode-languageserver API version in use.

// This handler provides the initial list of the completion items.
connection.onCompletion((): CompletionItem[] => {
  // The pass parameter contains the position of the text document in
  // which code complete got requested.
  const keywords = Object.values(KW);
  const completions: CompletionItem[] = keywords.map(keyword => ({
    label: keyword,
    kind: CompletionItemKind.Keyword,
    data: keyword,
  }));

  // Add common type completions
  const types = ['Text', 'Int', 'Bool', 'Float', 'User', 'Result', 'Option', 'Maybe'];
  types.forEach(type => {
    completions.push({
      label: type,
      kind: CompletionItemKind.Class,
      data: type,
    });
  });

  return completions;
});

// This handler resolves additional information for the item selected in
// the completion list.
connection.onCompletionResolve((item: CompletionItem): CompletionItem => {
  if (item.data === 'this module is') {
    item.detail = 'Module declaration';
    item.documentation = 'Declares the module name for this file';
  } else if (item.data === 'define') {
    item.detail = 'Type definition';
    item.documentation = 'Define a new data type or enum';
  } else if (item.data === 'to') {
    item.detail = 'Function definition';
    item.documentation = 'Define a new function';
  }
  return item;
});

connection.onHover(async params => {
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return null;
  const entry = getOrParse(doc);
  const { tokens: toks, ast } = entry;
  const pos = params.position;
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
      if ((decl as any).kind === 'Func') {
        const f = decl as AstFunc;
        const nameAt = tokenNameAt(toks as any[], pos);
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
      if ((decl as any).kind === 'Data') {
        const d = decl as AstData;
        const fields = d.fields.map(f => `${f.name}: ${typeText(f.type)}`).join(', ');
        return { contents: { kind: 'markdown', value: `type ${d.name}${fields ? ' — ' + fields : ''}` } };
      }
      if ((decl as any).kind === 'Enum') {
        const e = decl as AstEnum;
        return { contents: { kind: 'markdown', value: `enum ${e.name} — ${e.variants.join(', ')}` } };
      }
    }
  } catch {
    // ignore
  }
  return null;
});

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
          const ds: DocumentSymbol = {
            name: data.name,
            kind: SymbolKind.Struct,
            range: spanOrDoc((data as any).span, doc),
            selectionRange: spanOrDoc((data as any).span, doc),
            children: [],
            detail: 'type',
          };
          // fields
          for (const f of data.fields) {
            ds.children!.push({
              name: f.name,
              kind: SymbolKind.Field,
              range: spanOrDoc(((f as any).span as Span | undefined), doc),
              selectionRange: spanOrDoc(((f as any).span as Span | undefined), doc),
              detail: typeText(f.type),
            });
          }
          pushChild(moduleParent, ds);
          break;
        }
        case 'Enum': {
          const en = d as AstEnum;
          const es: DocumentSymbol = {
            name: en.name,
            kind: SymbolKind.Enum,
            range: spanOrDoc((en as any).span, doc),
            selectionRange: spanOrDoc((en as any).span, doc),
            children: [],
          };
          const vspans: (Span | undefined)[] = (((en as any).variantSpans as Span[] | undefined) || []);
          for (let vi = 0; vi < en.variants.length; vi++) {
            const v = en.variants[vi]!;
            const sp = vspans[vi];
            es.children!.push({ name: v, kind: SymbolKind.EnumMember, range: spanOrDoc(sp, doc), selectionRange: spanOrDoc(sp, doc) });
          }
          pushChild(moduleParent, es);
          break;
        }
        case 'Func': {
          const f = d as AstFunc;
          const fs: DocumentSymbol = {
            name: f.name,
            kind: SymbolKind.Function,
            range: spanOrDoc((f as any).span, doc),
            selectionRange: spanOrDoc((((f as any).nameSpan as Span | undefined) ?? (f as any).span), doc),
            children: [],
            detail: funcDetail(f),
          };
          // params
          for (const p of f.params) {
            fs.children!.push({
              name: p.name,
              kind: SymbolKind.Variable,
              range: spanOrDoc(((p as any).span as Span | undefined), doc),
              selectionRange: spanOrDoc(((p as any).span as Span | undefined), doc),
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

function spanOrDoc(span: Span | undefined, doc: TextDocument): Range {
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

function funcDetail(f: AstFunc): string {
  const eff = (f.effects || []).join(' ');
  const params = f.params.map(p => `${p.name}: ${typeText(p.type)}`).join(', ');
  const ret = typeText(f.retType);
  const effTxt = eff ? ` performs ${eff}` : '';
  return `(${params}) -> ${ret}${effTxt}`;
}

function typeText(t: AstType): string {
  switch (t.kind) {
    case 'TypeName':
      return t.name;
    case 'TypeVar':
      return t.name;
    case 'Maybe':
      return `Maybe<${typeText(t.type)}>`;
    case 'Option':
      return `Option<${typeText(t.type)}>`;
    case 'Result':
      return `Result<${typeText(t.ok)}, ${typeText(t.err)}>`;
    case 'List':
      return `List<${typeText(t.type)}>`;
    case 'Map':
      return `Map<${typeText(t.key)}, ${typeText(t.val)}>`;
    case 'TypeApp':
      return `${t.base}<${t.args.map(typeText).join(', ')}>`;
    case 'FuncType':
      return `(${t.params.map(typeText).join(', ')}) -> ${typeText(t.ret)}`;
    default:
      return 'Unknown';
  }
}

function within(span: Span | undefined, pos: { line: number; character: number }): boolean {
  if (!span) return false;
  const l = pos.line + 1, c = pos.character + 1;
  const s = span.start, e = span.end;
  if (l < s.line || l > e.line) return false;
  if (l === s.line && c < s.col) return false;
  if (l === e.line && c > e.col) return false;
  return true;
}

function findDeclAt(m: AstModule, pos: { line: number; character: number }): AstDecl | null {
  let found: AstDecl | null = null;
  for (const d of m.decls) {
    const sp: Span | undefined = (d as any).span;
    if (within(sp, pos)) {
      found = d as AstDecl;
      if ((d as any).kind === 'Func') return d as AstDecl;
    }
  }
  return found;
}

function tokenNameAt(tokens: readonly any[], pos: { line: number; character: number }): string | null {
  for (const t of tokens as any[]) {
    if (!t || !t.start || !t.end) continue;
    const span: Span = { start: { line: t.start.line, col: t.start.col }, end: { line: t.end.line, col: t.end.col } } as any;
    if (within(span, pos)) {
      if (t.kind === 'IDENT' || t.kind === 'TYPE_IDENT') return String(t.value || '');
    }
  }
  return null;
}

// No doc comment extraction in hover; kept minimal

// buildIdIndex moved to ./utils

// collectLetNames was superseded by findLocalLetWithExpr

// Semantic tokens (basic): keywords, TYPE_IDENTs, plus simple AST-derived kinds
connection.languages.semanticTokens.on((params: SemanticTokensParams): SemanticTokens => {
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return { data: [] };
  const entry = getOrParse(doc);
  const { tokens: toks, ast } = entry;
  try {
    const ast2 = (ast as AstModule) || (parse(toks) as AstModule);
    const builder: number[] = [];
    let prevLine = 0;
    let prevChar = 0;
    const push = (line0: number, char0: number, length: number, type: number, modifiers = 0): void => {
      const dl = builder.length === 0 ? line0 : line0 - prevLine;
      const dc = builder.length === 0 ? char0 : line0 === prevLine ? char0 - prevChar : char0;
      builder.push(dl, dc, Math.max(0, length), type, modifiers);
      prevLine = line0;
      prevChar = char0;
    };
    const typesIndex = tokenTypeIndexMap();
    const modsIndex = tokenModIndexMap();

    // Token-based coloring: keywords and TYPE_IDENT
    for (const t of toks) {
      if (!t || !t.start || !t.end) continue;
      const line0 = (t.start.line - 1) | 0;
      const char0 = (t.start.col - 1) | 0;
      const len = Math.max(0, (t.end.col - t.start.col) | 0);
      if (t.kind === TokenKind.KEYWORD) push(line0, char0, len, typesIndex['keyword'] ?? 0, 0);
      else if (t.kind === TokenKind.TYPE_IDENT) push(line0, char0, len, typesIndex['type'] ?? 0, 0);
    }

    // AST-based: function decl spans, data/enum decl spans, let locals
    const addSpan = (sp: Span | undefined, type: number, mods = 0): void => {
      if (!sp) return;
      const line0 = sp.start.line - 1;
      const char0 = sp.start.col - 1;
      const len = Math.max(0, sp.end.col - sp.start.col);
      if (len > 0) push(line0, char0, len, type, mods);
    };
    for (const d of (ast2.decls as AstDecl[])) {
      if (d.kind === 'Func') {
        addSpan((d as any).span, typesIndex['function'] ?? 0);
        // Prefer highlighting function name itself if available
        const nsp = ((d as any).nameSpan as Span | undefined);
        if (nsp) addSpan(nsp, typesIndex['function'] ?? 0, 1 << (modsIndex['declaration'] ?? 0));
        const f = d as AstFunc;
        // parameters
        for (const p of f.params) addSpan(((p as any).span as Span | undefined), typesIndex['parameter'] ?? 0, 1 << (modsIndex['declaration'] ?? 0));
        if (f.body) {
          const lets = collectLetsWithSpan(f.body);
          for (const [, sp] of lets) addSpan(sp, typesIndex['variable'] ?? 0);
        }
      } else if (d.kind === 'Data') {
        addSpan((d as any).span, typesIndex['type'] ?? 0);
        // fields
        for (const f of (d as AstData).fields) addSpan(((f as any).span as Span | undefined), typesIndex['property'] ?? 0, 1 << (modsIndex['declaration'] ?? 0));
      } else if (d.kind === 'Enum') {
        addSpan((d as any).span, typesIndex['enum'] ?? 0);
        const vspans: (Span | undefined)[] = (((d as any).variantSpans as Span[] | undefined) || []);
        for (const sp of vspans) addSpan(sp, typesIndex['enumMember'] ?? 0, 1 << (modsIndex['declaration'] ?? 0));
      }
    }
    return { data: builder };
  } catch {
    return { data: [] };
  }
});

const SEM_LEGEND: SemanticTokensLegend = {
  tokenTypes: ['keyword', 'type', 'function', 'parameter', 'variable', 'enum', 'enumMember', 'property'],
  tokenModifiers: ['declaration'],
};
const TOKEN_TYPE_INDEX: Record<string, number> = Object.fromEntries(
  SEM_LEGEND.tokenTypes.map((t, i) => [t, i])
);
function tokenTypeIndexMap(): Record<string, number> { return TOKEN_TYPE_INDEX; }
const TOKEN_MOD_INDEX: Record<string, number> = Object.fromEntries(
  SEM_LEGEND.tokenModifiers.map((t, i) => [t, i])
);
function tokenModIndexMap(): Record<string, number> { return TOKEN_MOD_INDEX; }

function collectBlockSymbols(b: AstBlock, parent: DocumentSymbol, doc: TextDocument): void {
  for (const s of b.statements as AstStmt[]) {
    if (s.kind === 'Let') {
      const letS = s as AstLet;
      parent.children!.push({
        name: letS.name,
        kind: SymbolKind.Variable,
        range: spanOrDoc((letS as any).span, doc),
        selectionRange: spanOrDoc((letS as any).span, doc),
      });
    } else if (s.kind === 'Block') {
      const blk = s as AstBlock;
      const bs: DocumentSymbol = {
        name: 'block',
        kind: SymbolKind.Namespace,
        range: spanOrDoc((blk as any).span, doc),
        selectionRange: spanOrDoc((blk as any).span, doc),
        children: [],
      };
      collectBlockSymbols(blk, bs, doc);
      parent.children!.push(bs);
    } else if (s.kind === 'If') {
      // Collect nested blocks
      const thenB = s.thenBlock as AstBlock;
      const thenS: DocumentSymbol = {
        name: 'if',
        kind: SymbolKind.Namespace,
        range: spanOrDoc((s as any).span, doc),
        selectionRange: spanOrDoc((s as any).span, doc),
        children: [],
      };
      collectBlockSymbols(thenB, thenS, doc);
      if (s.elseBlock) collectBlockSymbols(s.elseBlock as AstBlock, thenS, doc);
      parent.children!.push(thenS);
    } else if (s.kind === 'Match') {
      const ms: DocumentSymbol = {
        name: 'match',
        kind: SymbolKind.Namespace,
        range: spanOrDoc((s as any).span, doc),
        selectionRange: spanOrDoc((s as any).span, doc),
        children: [],
      };
      for (const c of s.cases) {
        if (c.body.kind === 'Block') collectBlockSymbols(c.body as AstBlock, ms, doc);
      }
      parent.children!.push(ms);
    }
  }
}

// Go to definition: functions/types/params/locals (single-file)
connection.onDefinition(params => {
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return null;
  const { tokens: toks, ast } = getOrParse(doc);
  try {
    const ast2 = (ast as AstModule) || (parse(toks) as AstModule);
    const name = tokenNameAt(toks as any[], params.position);
    if (!name) return null;

    // Index top-level decls
    const declMap = new Map<string, Span | undefined>();
    for (const d of ast2.decls as AstDecl[]) {
      if (d.kind === 'Func' || d.kind === 'Data' || d.kind === 'Enum') {
        // Prefer function nameSpan when present
        const nm = (d as any).name as string;
        const nsp = (d as any).nameSpan as Span | undefined;
        declMap.set(nm, (nsp ?? ((d as any).span as Span | undefined)));
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
      const rec = indexByModule.get(mod);
      if (rec) {
        const d = rec.decls.find(dd => dd.name === mem);
        const sp = d?.nameSpan || d?.span;
        if (sp) return toLocation(rec.uri, sp);
      }
    }

    // If inside a function, check params and lets
    const here = findDeclAt(ast2, params.position);
    if (here && (here as any).kind === 'Func') {
      const f = here as AstFunc;
      // params
      const pHit = f.params.find(p => p.name === name);
      if (pHit) {
        const psp = ((pHit as any).span as Span | undefined) || ((f as any).span as Span | undefined);
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

function toLocation(uri: string, sp: Span): Location {
  return {
    uri,
    range: {
      start: { line: sp.start.line - 1, character: sp.start.col - 1 },
      end: { line: sp.end.line - 1, character: sp.end.col - 1 },
    },
  };
}

function collectLetsWithSpan(b: AstBlock | null): Map<string, Span> {
  const out = new Map<string, Span>();
  if (!b) return out;
  for (const s of b.statements as AstStmt[]) {
    if (s.kind === 'Let') out.set((s as AstLet).name, (s as any).span as Span);
    else if (s.kind === 'If') {
      collectLetsWithSpan(s.thenBlock as AstBlock).forEach((v, k) => out.set(k, v));
      if (s.elseBlock) collectLetsWithSpan(s.elseBlock as AstBlock).forEach((v, k) => out.set(k, v));
    } else if (s.kind === 'Match') {
      for (const c of s.cases) if (c.body.kind === 'Block') collectLetsWithSpan(c.body as AstBlock).forEach((v, k) => out.set(k, v));
    } else if (s.kind === 'Block') collectLetsWithSpan(s as unknown as AstBlock).forEach((v, k) => out.set(k, v));
  }
  return out;
}

// Return the binding name if the given name is bound by a match pattern whose body contains the hover position
// findPatternBindingInScope replaced by findPatternBindingDetail

function enumVariantSpanMap(m: AstModule): Map<string, Span> {
  const out = new Map<string, Span>();
  for (const d of m.decls as AstDecl[]) {
    if (d.kind === 'Enum') {
      const en = d as AstEnum;
      const vspans: (Span | undefined)[] = (((en as any).variantSpans as Span[] | undefined) || []);
      for (let i = 0; i < en.variants.length; i++) {
        const nm = en.variants[i]!;
        const sp = vspans[i];
        if (sp) out.set(nm, sp);
      }
    }
  }
  return out;
}

function dataFieldSpanMap(m: AstModule): Map<string, Span> {
  const out = new Map<string, Span>();
  for (const d of m.decls as AstDecl[]) {
    if (d.kind === 'Data') {
      const da = d as AstData;
      for (const f of da.fields) {
        const sp = (f as any).span as Span | undefined;
        if (sp) out.set(`${da.name}.${f.name}`, sp);
      }
    }
  }
  return out;
}

function findConstructFieldAt(m: AstModule, pos: { line: number; character: number }): { typeName: string; field: string } | null {
  // Shallow walk function bodies to find Construct nodes and match field spans
  const withinSpan = (sp: Span | undefined): boolean => within(sp, pos);
  for (const d of m.decls as AstDecl[]) {
    if (d.kind !== 'Func') continue;
    const f = d as AstFunc;
    const walkBlock = (b: AstBlock): void => {
      for (const s of b.statements as AstStmt[]) {
        if (s.kind === 'Return') {
          walkExpr((s as any).expr);
        } else if (s.kind === 'Let' || s.kind === 'Set') {
          walkExpr((s as any).expr);
        } else if (s.kind === 'If') {
          walkExpr(s.cond as any);
          walkBlock(s.thenBlock as AstBlock);
          if (s.elseBlock) walkBlock(s.elseBlock as AstBlock);
        } else if (s.kind === 'Match') {
          walkExpr(s.expr as any);
          for (const c of s.cases) {
            if (c.body.kind === 'Block') walkBlock(c.body as AstBlock);
          }
        } else if (s.kind === 'Block') {
          walkBlock(s as unknown as AstBlock);
        }
      }
    };
    const walkExpr = (e: any): void => {
      if (!e || !e.kind) return;
      if (e.kind === 'Construct') {
        for (const fld of e.fields || []) {
          const sp = (fld as any).span as Span | undefined;
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

function findLocalLetWithExpr(b: AstBlock | null, name: string): { span: Span; expr: any } | null {
  if (!b) return null;
  for (const s of b.statements as AstStmt[]) {
    if (s.kind === 'Let' && (s as AstLet).name === name) {
      return { span: (s as any).span as Span, expr: (s as any).expr };
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
    } else if (s.kind === 'Block') {
      const r = findLocalLetWithExpr(s as unknown as AstBlock, name);
      if (r) return r;
    }
  }
  return null;
}

// exprTypeText moved to ./utils

function findPatternBindingDetail(fn: AstFunc, name: string, pos: { line: number; character: number }): { name: string; ofType?: string | undefined } | null {
  const inRange = (sp?: Span): boolean => within(sp, pos);
  if (!fn.body) return null;
  const walkBlock = (b: AstBlock): { name: string; ofType?: string | undefined } | null => {
    for (const s of b.statements as AstStmt[]) {
      if (s.kind === 'Match') {
        const m = s as any;
        for (const c of m.cases) {
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
          const body = c.body as any;
          const sp = (body as any).span as Span | undefined;
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
      } else if (s.kind === 'Block') {
        const a = walkBlock(s as unknown as AstBlock);
        if (a) return a;
      }
    }
    return null;
  };
  return walkBlock(fn.body);
}

// (Removed duplicate single-file find-references and rename handlers to favor
// the workspace-aware implementations earlier in this file.)

// Quick fixes / hints for ambiguous interop calls
connection.onCodeAction(params => {
  const actions: any[] = [];
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return actions;
  const fsPath = uriToFsPath(doc.uri) || doc.uri;
  const suggestModuleFromPath = (): string => {
    try {
      const path = require('node:path');
      const root = process.cwd();
      const rel = path.relative(root, fsPath);
      const noExt = rel.replace(/\.[^.]+$/, '');
      const parts = noExt.split(path.sep).filter(Boolean);
      // If under cnl/, drop leading segment
      if (parts[0] === 'cnl') parts.shift();
      return parts.join('.').replace(/[^A-Za-z0-9_.]/g, '_') || 'main';
    } catch {
      return 'main';
    }
  };
  for (const d of params.context.diagnostics) {
    if (typeof d.message === 'string' && d.message.startsWith('Ambiguous interop call')) {
      // Advisory hint
      actions.push({
        title: 'Hint: Disambiguate numeric overload (use 1L or 1.0) — see Guide: JVM Interop Overloads',
        kind: 'quickfix',
        diagnostics: [d],
      });
      // Compute concrete edits from current document content
      try {
        const { tokens: toks } = getOrParse(doc);
        const edits = computeDisambiguationEdits(toks, d.range);
        if (edits.length > 0) {
          actions.push({
            title: 'Fix: Make numeric literals unambiguous',
            kind: 'quickfix',
            diagnostics: [d],
            edit: {
              changes: {
                [params.textDocument.uri]: edits,
              },
            },
          });
        }
      } catch {
        // ignore
      }
    }
    // Quick fix for nullability: replace null with "" for Text.* calls
    if (typeof d.message === 'string' && d.message.startsWith('Nullability:')) {
      // Parse dotted name and param index from diagnostic message
      const m = d.message.match(/parameter\s+(\d+)\s+of\s+'([^']+)'/);
      const paramIdx = m ? Math.max(1, parseInt(m[1] || '1', 10)) : 1;
      const dotted = m ? m[2] || '' : '';
      const suggest = (dn: string, idx: number): string => {
        // Text helpers
        if (dn === 'Text.split') return idx === 2 ? '" "' : '""'; // h, sep
        if (dn === 'Text.startsWith') return '""'; // param 1 or 2
        if (dn === 'Text.endsWith') return '""';   // param 1 or 2
        if (dn === 'Text.indexOf') return idx === 2 ? '" "' : '""'; // h, needle
        if (dn === 'Text.contains') return '""';
        if (dn === 'Text.replace') return '""';    // any of 1/2/3
        if (dn === 'Text.toUpper' || dn === 'Text.toLower' || dn === 'Text.length') return '""';
        if (dn === 'Text.concat') return '""';
        // Collections / Interop defaults
        if (dn === 'List.get' && idx === 2) return '0';
        if (dn === 'Map.get' && idx === 2) return '""';
        if (dn === 'Map.containsKey' && idx === 2) return '""';
        if (dn === 'Set.contains' && idx === 2) return '""';
        if (dn === 'Set.add' && idx === 2) return '""';
        if (dn === 'Set.remove' && idx === 2) return '""';
        if (dn === 'aster.runtime.Interop.sum') return '0';
        if (dn === 'aster.runtime.Interop.pick') return '""';
        // fallback for Text.*
        if (dn.startsWith('Text.')) return '""';
        return 'null';
      };
      const replacement = suggest(dotted, paramIdx);
      if (replacement !== 'null') {
        actions.push({
          title: `Fix: Replace null with ${replacement}`,
          kind: 'quickfix',
          diagnostics: [d],
          edit: {
            changes: {
              [params.textDocument.uri]: [
                {
                  range: d.range,
                  newText: replacement,
                },
              ],
            },
          },
        });
      }
    }
    // Quick fix: add missing module header
    if (typeof d.message === 'string' && d.message.startsWith('Missing module header')) {
      const mod = suggestModuleFromPath();
      const header = `This module is ${mod}.\n`;
      actions.push({
        title: `Fix: Add module header (This module is ${mod}.)`,
        kind: 'quickfix',
        diagnostics: [d],
        edit: { changes: { [params.textDocument.uri]: [TextEdit.insert({ line: 0, character: 0 }, header)] } },
      });
    }
    // Quick fix: add missing punctuation at end of line for simple statements
    if (typeof d.message === 'string' && /Expected (':'|\.) at end of line/i.test(d.message)) {
      const rng = d.range;
      const isColon = /:/.test(d.message);
      const ch = isColon ? ':' : '.';
      actions.push({
        title: `Fix: add '${ch}' at end of line`,
        kind: 'quickfix',
        diagnostics: [d],
        edit: { changes: { [params.textDocument.uri]: [TextEdit.insert(rng.end, ch)] } },
      });
    }
  }
  // Bulk numeric overload disambiguation for current selection (no diagnostic required)
  try {
    const { tokens: toks } = getOrParse(doc);
    const edits = computeDisambiguationEdits(toks, params.range as any);
    if (edits.length > 0) {
      actions.push({
        title: 'Fix: Disambiguate numeric overloads in selection',
        kind: 'quickfix',
        edit: { changes: { [params.textDocument.uri]: edits } },
      });
    }
  } catch {
    // ignore
  }
  return actions;
});

// Make the text document manager listen on the connection
// for open, change and close text document events
documents.listen(connection);

// Listen on the connection
connection.listen();
function tryLoadPersistedIndex(): void {
  try {
    if (!indexPersistEnabled) return;
    const root = process.cwd();
    const p = indexPathOverride || require('node:path').join(root, '.asteri', 'lsp-index.json');
    const fs = require('node:fs');
    if (!fs.existsSync(p)) return;
    const json = JSON.parse(fs.readFileSync(p, 'utf8')) as { files?: any[] };
    if (!json || !Array.isArray(json.files)) return;
    for (const f of json.files) {
      const rec = { uri: f.uri as string, moduleName: (f.moduleName as string) || null, decls: f.decls as any[] };
      indexByUri.set(rec.uri, rec);
      if (rec.moduleName) indexByModule.set(rec.moduleName, rec);
    }
    connection.console.log(`Loaded persisted index: ${json.files.length} files.`);
  } catch (e: any) {
    connection.console.warn(`Failed to load persisted index: ${e?.message ?? String(e)}`);
  }
}
