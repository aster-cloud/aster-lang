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
  CompletionItem,
  CompletionItemKind,
  TextDocumentSyncKind,
  InitializeResult,
  CodeActionKind,
  type CodeAction,
  type CodeActionParams,
  TextEdit,
  type Range,
  // DocumentDiagnosticReportKind,
  // type DocumentDiagnosticReport,
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
import { DiagnosticError } from '../diagnostics.js';
import { KW } from '../tokens.js';
// import { lowerModule } from "../lower_to_core";

// Create a connection for the server, using Node's IPC as a transport.
const connection = createConnection(ProposedFeatures.all);

// Create a simple text document manager.
const documents: TextDocuments<TextDocument> = new TextDocuments(TextDocument);

let hasConfigurationCapability = false;
let hasWorkspaceFolderCapability = false;
let hasDiagnosticRelatedInformationCapability = false;

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
        workspaceDiagnostics: false,
      },
      codeActionProvider: {
        codeActionKinds: [CodeActionKind.QuickFix],
      },
      hoverProvider: true,
      documentSymbolProvider: true,
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
});

// The example settings
interface AsterSettings {
  maxNumberOfProblems: number;
}

// The global settings, used when the `workspace/configuration` request is not supported by the client.
const defaultSettings: AsterSettings = { maxNumberOfProblems: 1000 };
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

  // Revalidate all open text documents
  documents.all().forEach(validateTextDocument);
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
  validateTextDocument(change.document);
});

async function validateTextDocument(textDocument: TextDocument): Promise<void> {
  void (await getDocumentSettings(textDocument.uri));
  const text = textDocument.getText();
  const diagnostics: Diagnostic[] = [];

  try {
    const canonicalized = canonicalize(text);
    const tokens = lex(canonicalized);
    const ast = parse(tokens);
    // Lightweight lexical checks for interop
    diagnostics.push(...findAmbiguousInteropCalls(tokens));
    diagnostics.push(...(await import('./analysis.js')).findNullabilityDiagnostics(tokens));
    // Also run core + typecheck to surface semantic warnings
    try {
      const core = (await import('../lower_to_core.js')).lowerModule(ast);
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

  // Send the computed diagnostics to VSCode.
  connection.sendDiagnostics({ uri: textDocument.uri, diagnostics });
}

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
  const text = doc.getText();
  const can = canonicalize(text);
  const toks = lex(can);
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
  return null;
});

connection.onDocumentSymbol(() => {
  // TODO: Implement document symbol provider
  return [];
});

// Quick fixes / hints for ambiguous interop calls
connection.onCodeAction(params => {
  const actions: any[] = [];
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return actions;
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
        const text = doc.getText();
        const can = canonicalize(text);
        const toks = lex(can);
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
  }
  return actions;
});

// Make the text document manager listen on the connection
// for open, change and close text document events
documents.listen(connection);

// Listen on the connection
connection.listen();
