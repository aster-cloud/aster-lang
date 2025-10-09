#!/usr/bin/env node

// Basic LSP server foundation for Aster CNL

import {
  createConnection,
  TextDocuments,
  ProposedFeatures,
  InitializeParams,
  DidChangeConfigurationNotification,
  DidChangeWatchedFilesNotification,
  TextDocumentSyncKind,
  InitializeResult,
  CodeActionKind,
} from 'vscode-languageserver/node.js';

import { promises as fsPromises } from 'node:fs';

import { TextDocument } from 'vscode-languageserver-textdocument';

import { canonicalize } from '../canonicalizer.js';
import { lex } from '../lexer.js';
import { parse } from '../parser.js';
import type {
  Module as AstModule,
  Span,
} from '../types.js';
import { buildIdIndex, exprTypeText } from './utils.js';
import {
  getModuleIndex,
  getAllModules,
  updateDocumentIndex,
  invalidateDocument,
  loadIndex,
  saveIndex,
  setIndexConfig,
  rebuildWorkspaceIndex,
} from './index.js';
import {
  registerDiagnosticHandlers,
  setDiagnosticConfig,
} from './diagnostics.js';
import { registerCompletionHandlers, typeText } from './completion.js';
import {
  registerNavigationHandlers,
  uriToFsPath,
  ensureUri,
  offsetToPos,
  tokenNameAt,
  collectLetsWithSpan,
} from './navigation.js';
import { registerFormattingHandlers } from './formatting.js';
import { registerCodeActionHandlers } from './codeaction.js';
import { registerSymbolsHandlers } from './symbols.js';
import { registerTokensHandlers, SEM_LEGEND } from './tokens.js';
import { registerHealthHandlers } from './health.js';
// import { lowerModule } from "../lower_to_core";

// Create a connection for the server, using Node's IPC as a transport.
const connection = createConnection(ProposedFeatures.all);

// Create a simple text document manager.
const documents: TextDocuments<TextDocument> = new TextDocuments(TextDocument);

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
let currentIndexPath: string | null = null;
let indexPersistenceActive = true;
const workspaceFolders: string[] = [];

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
  return entry;
}

let hasConfigurationCapability = false;
let hasWorkspaceFolderCapability = false;
let hasDiagnosticRelatedInformationCapability = false;
let hasWatchedFilesCapability = false;
let watcherRegistered = false;

connection.onInitialize(async (params: InitializeParams) => {
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

  // Initialize diagnostics module configuration
  setDiagnosticConfig({
    relatedInformationSupported: hasDiagnosticRelatedInformationCapability,
    workspaceDiagnosticsEnabled: true,
    capabilityManifestPath: process.env.ASTER_CAPS || null,
  });

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
      documentHighlightProvider: true,
      signatureHelpProvider: {
        triggerCharacters: ['(', ','],
        retriggerCharacters: [',', ')'],
      },
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
      // Provider 能力声明（对应已实现的功能）
      definitionProvider: true,
      referencesProvider: true,
      renameProvider: { prepareProvider: true },
      documentLinkProvider: { resolveProvider: false },
    },
  };
  if (hasWorkspaceFolderCapability) {
    result.capabilities.workspace = {
      workspaceFolders: {
        supported: true,
      },
    };
  }
  try {
    const path = require('node:path');
    const candidateRoots: Array<string | null | undefined> = [];
    // Collect workspace folders for later index rebuild
    if (Array.isArray(params.workspaceFolders) && params.workspaceFolders.length > 0) {
      for (const folder of params.workspaceFolders) {
        const fsPath = uriToFsPath(folder.uri);
        if (fsPath) {
          workspaceFolders.push(fsPath);
          candidateRoots.push(fsPath);
        }
      }
    }
    if (params.rootUri) candidateRoots.push(uriToFsPath(params.rootUri));
    candidateRoots.push(params.rootPath);
    candidateRoots.push(process.cwd());
    const root = candidateRoots.find(r => typeof r === 'string' && r.length > 0) as string | undefined;
    // Fallback: if no workspace folders collected, use root
    if (workspaceFolders.length === 0 && root) {
      workspaceFolders.push(root);
    }
    currentIndexPath = root ? path.join(root, '.asteri', 'lsp-index.json') : null;
    setIndexConfig({ persistEnabled: true, indexPath: currentIndexPath ?? null, autoSaveDelay: 500 });
    indexPersistenceActive = true;
    if (currentIndexPath) {
      const loaded = await loadIndex(currentIndexPath);
      if (loaded) {
        connection.console.log(`Loaded persisted index from ${path.relative(root ?? process.cwd(), currentIndexPath)}`);
      }
    }
  } catch (error: any) {
    connection.console.warn(`Index initialization failed: ${error?.message ?? String(error)}`);
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
    connection.onDidChangeWatchedFiles(async ev => {
      try {
        for (const ch of ev.changes) {
          const path = uriToFsPath(ch.uri);
          if (!path) continue;
          try {
            const text = await fsPromises.readFile(path, 'utf8');
            const doc = TextDocument.create(ch.uri, 'cnl', 0, text);
            await updateDocumentIndex(doc.uri, doc.getText()).catch(() => {});
          } catch {
            // File doesn't exist or can't be read
            invalidateDocument(ch.uri);
          }
        }
      } catch {
        // ignore
      }
    });
  } else {
    connection.console.warn('Client does not advertise didChangeWatchedFiles; workspace index updates may be limited to open documents.');
  }
  // Rebuild workspace index in background for instant symbol search
  if (workspaceFolders.length > 0) {
    void (async (): Promise<void> => {
      try {
        await rebuildWorkspaceIndex(workspaceFolders);
        connection.console.log(`Workspace index rebuilt: ${getAllModules().length} modules indexed`);
      } catch (error: any) {
        connection.console.warn(`Workspace index rebuild failed: ${error?.message ?? String(error)}`);
      }
    })();
  }

  // Register health handlers
  registerHealthHandlers(connection, hasWatchedFilesCapability, watcherRegistered, getAllModules);

  // Register diagnostic handlers
  registerDiagnosticHandlers(connection, documents, getOrParse);

  // Register completion handlers
  registerCompletionHandlers(connection, documents, getOrParse);

  // Register navigation handlers
  registerNavigationHandlers(connection, documents, getOrParse, getDocumentSettings);

  // Register formatting handlers
  registerFormattingHandlers(connection, documents, getDocumentSettings);

  // Register code action handlers
  registerCodeActionHandlers(connection, documents, getOrParse, uriToFsPath);

  // Register symbols handlers
  registerSymbolsHandlers(connection, documents, getAllModules, ensureUri, offsetToPos);

  // Register tokens handlers (semantic tokens, inlay hints, document highlight)
  registerTokensHandlers(connection, documents, getOrParse, typeText, exprTypeText, tokenNameAt, collectLetsWithSpan);
});

// The example settings
interface AsterSettings {
  maxNumberOfProblems: number;
  format?: { mode?: 'lossless' | 'normalize'; reflow?: boolean };
  index?: { persist?: boolean; path?: string };
  rename?: { scope?: 'open' | 'workspace' };
  diagnostics?: { workspace?: boolean };
  streaming?: { referencesChunk?: number; renameChunk?: number; logChunks?: boolean };
}

// The global settings, used when the `workspace/configuration` request is not supported by the client.
const defaultSettings: AsterSettings = { maxNumberOfProblems: 1000, format: { mode: 'lossless', reflow: true }, index: { persist: true }, rename: { scope: 'workspace' }, diagnostics: { workspace: true }, streaming: { referencesChunk: 200, renameChunk: 200, logChunks: false } };
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
  // Update diagnostics and index settings
  getDocumentSettings('').then(s => {
    setDiagnosticConfig({
      workspaceDiagnosticsEnabled: s.diagnostics?.workspace ?? true,
    });
    const persistEnabled = s.index?.persist ?? true;
    if (typeof s.index?.path === 'string' && s.index.path.length > 0) {
      currentIndexPath = s.index.path;
    }
    setIndexConfig({
      persistEnabled,
      indexPath: currentIndexPath ?? null,
    });
    indexPersistenceActive = persistEnabled;
  }).catch(() => {
    setDiagnosticConfig({
      workspaceDiagnosticsEnabled: true,
    });
    setIndexConfig({
      persistEnabled: true,
      indexPath: currentIndexPath ?? null,
    });
    indexPersistenceActive = true;
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
    void updateDocumentIndex(change.document.uri, change.document.getText()).catch(() => {});
  } catch {
    // ignore
  }
});

documents.onDidSave(e => {
  try {
    void updateDocumentIndex(e.document.uri, e.document.getText()).catch(() => {});
  } catch {
    // ignore
  }
});

// Range formatting provider (lossless with minimal seam reflow)
// Formatting handlers (rangeFormatting, documentFormatting) moved to ./formatting.js
documents.onDidClose(e => {
  docCache.delete(e.document.uri);
  try {
    const existing = getModuleIndex(e.document.uri);
    if (existing) invalidateDocument(e.document.uri);
  } catch {}
});

// Workspace symbols and document links handlers moved to ./symbols.js

// Navigation handlers (references, rename, hover, symbols, definition) moved to ./navigation.js
// Helper functions (captureWordAt, findTokenPositionsSafe, etc.) also moved to ./navigation.js

// Inlay hints handler moved to ./tokens.js

// CodeAction handlers (effect declarations, capability manifest, interop fixes, etc.) moved to ./codeaction.js

// toGuideUri helper function moved to ./symbols.js

// onHover handler moved to ./navigation.js


// Document highlight handler moved to ./tokens.js

// Navigation helper functions (spanOrDoc, funcDetail, within, findDeclAt, tokenNameAt, etc.) moved to ./navigation.js

// Semantic tokens handler and helper functions (SEM_LEGEND, tokenTypeIndexMap, tokenModIndexMap) moved to ./tokens.js

// Go to definition: functions/types/params/locals (single-file)
// AST query helpers (collectBlockSymbols, toLocation, collectLetsWithSpan, enumVariantSpanMap,
// dataFieldSpanMap, findConstructFieldAt, findLocalLetWithExpr, findPatternBindingDetail) moved to ./navigation.js

// Quick fixes / hints for ambiguous interop calls
// Second CodeAction handler (merged with first into ./codeaction.js)

// Make the text document manager listen on the connection
// for open, change and close text document events
documents.listen(connection);

connection.onExit(() => {
  if (!currentIndexPath || !indexPersistenceActive) return;
  void saveIndex(currentIndexPath).catch(() => {});
});

// Listen on the connection
connection.listen();
