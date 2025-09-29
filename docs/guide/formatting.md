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

### Commands Provided by the Server

- Format Document: uses the configured mode to format the entire file.
- Format Selection/Range: formats only the highlighted range using the same mode.

## CLI Usage

Use the built CLI to format arbitrary `.cnl` files.

```bash
# Strict normalize mode (overwrite file)
npm run format:file -- --write path/to/file.cnl

# Lossless print to stdout (preserve whitespace/comments)
npm run format:file -- --lossless path/to/file.cnl

# Lossless with minimal seam reflow to stdout
npm run format:file -- --lossless --lossless-reflow path/to/file.cnl

# Lossless reflow (overwrite file)
npm run format:file -- --write --lossless --lossless-reflow path/to/file.cnl
```

## Examples & CI

- Normalize examples in the repo: `npm run fmt:examples` (CI default).
- Lossless example check (non‑blocking in CI): `npm run test:lossless`.

## Notes

- Lossless mode preserves user trivia and is ideal for editor saves that should
  not reflow the entire file.
- Normalize mode ensures examples and documentation stay consistent and
  canonicalized for reviews and CI.

