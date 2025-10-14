---
title: Formatting, LSP, and CLI
---

# Formatting, LSP, and CLI

This page shows how to configure the Aster LSP formatter in editors and how to
format files from the command line using both strict (normalize) and lossless
printing modes.

## Formatting Modes

- Normalize (strict):
  - Rewrites to strict CNL (effects in headers, comma‑separated params, etc.).
  - Deterministic; used by CI and `npm run fmt:examples`.

- Lossless (trivia‑preserving):
  - Re‑emits the original bytes via the CST, preserving whitespace/comments.
  - Optional minimal seam reflow (e.g., `. :` → `:`, remove spaces before punctuation) when enabled.

## VS Code Setup (LSP)

1. Ensure the Aster LSP server (`aster-lsp`) is available (via extension or a custom client).
2. Add these settings to `settings.json`:

```json
{
  "editor.formatOnSave": true,
  // Choose formatter mode (lossless preserves trivia; normalize is strict)
  "asterLanguageServer.format.mode": "lossless", // or "normalize"
  // Apply minimal seam fixes in lossless mode
"asterLanguageServer.format.reflow": true
}
```

Additional LSP settings:

- `asterLanguageServer.index.persist` (default: true) — persist a workspace index to `.asteri/lsp-index.json`.
- `asterLanguageServer.index.path` (optional) — override index path.
- `asterLanguageServer.rename.scope` (default: `workspace`) — limit rename to `open` or allow `workspace`.
- `asterLanguageServer.diagnostics.workspace` (default: true) — enable workspace diagnostics across indexed files.
- `asterLanguageServer.streaming.referencesChunk` (default: 200) — chunk size for streaming references; smaller values increase the frequency of progress updates.
- `asterLanguageServer.streaming.renameChunk` (default: 200) — chunk size for rename progress updates during large workspace scans.
- `asterLanguageServer.streaming.logChunks` (default: false) — always log chunk sizes to the LSP output (window/logMessage) to aid debugging/smoke tests.

### Commands Provided by the Server

- Format Document: uses the configured mode to format the entire file.
- Format Selection/Range: formats only the highlighted range using the same mode.

## CLI Usage

Use the built CLI to format arbitrary `.aster` files.

```bash
# Strict normalize mode (overwrite file)
npm run format:file -- --write path/to/file.aster

# Lossless print to stdout (preserve whitespace/comments)
npm run format:file -- --lossless path/to/file.aster

# Lossless with minimal seam reflow to stdout
npm run format:file -- --lossless --lossless-reflow path/to/file.aster

# Lossless reflow (overwrite file)
npm run format:file -- --write --lossless --lossless-reflow path/to/file.aster
```

## Client-side File Watch (VS Code Extension)

If you write a custom VS Code extension to host the Aster LSP, register file
watchers so the server can update its workspace index when CNL files change on
disk:

```ts
// In your VS Code client extension
import { workspace } from 'vscode';
import {
  DidChangeWatchedFilesNotification,
} from 'vscode-languageclient/node';

// After creating and starting the client
client.onReady().then(() => {
  // Inform server to watch all .aster files in the workspace (if supported)
  client.sendNotification(DidChangeWatchedFilesNotification.type, {
    watchers: [{ globPattern: '**/*.aster' }],
  } as any);
});

// Or via VS Code native watchers
const watcher = workspace.createFileSystemWatcher('**/*.aster');
watcher.onDidChange(uri => {/* optional client-side handling */});
watcher.onDidCreate(uri => {/* optional */});
watcher.onDidDelete(uri => {/* optional */});
```

Note: The Aster server attempts to register watchers dynamically when the client
advertises `workspace.didChangeWatchedFiles.dynamicRegistration`. If the client
does not support watched files, the server logs a warning and will only
re-index open documents or explicit saves.

## LSP Health Request

The server exposes a custom health request to inspect watcher state and index size:

Method: `aster/health`

VS Code client example:

```ts
client.onReady().then(async () => {
  const health = await client.sendRequest('aster/health');
  console.log('Aster LSP health:', health);
});
```

Node (standalone) example using vscode-languageclient is similar; if you have a
raw JSON-RPC client, send a request like:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "aster/health",
  "params": {}
}
```

The response has the form:

```json
{
  "watchers": { "capability": true, "registered": true },
  "index": { "files": 42, "modules": 39 }
}
```

## LSP Code Actions (Quick Fixes)

The language server offers several helpful Quick Fixes:

- Disambiguate numeric overloads: converts `1`/`2` to `1L`/`2.0` as needed in a selection.
- Insert capability headers: add or remove “It performs IO/CPU” on `To …` lines based on diagnostics.
- Update capability manifest: write allow‑list entries (`io`/`cpu`) for a function or module.
- Add missing module header: inserts `This module is <name>.` inferred from the file path.
- Add missing punctuation: inserts `:` or `.` at end of line when required by syntax.

## Examples & CI

- Normalize examples in the repo: `npm run fmt:examples` (CI default).
- Lossless example check (non‑blocking in CI): `npm run test:lossless`.

## Notes

- Lossless mode preserves user trivia and is ideal for editor saves that should
  not reflow the entire file.
- Normalize mode ensures examples and documentation stay consistent and
  canonicalized for reviews and CI.
