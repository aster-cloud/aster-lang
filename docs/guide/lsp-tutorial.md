---
title: LSP Quick Fix Tutorial
---

# LSP Quick Fix Tutorial

This short walkthrough shows how to use Aster’s LSP code actions (Quick Fixes) on a small, imperfect file and iteratively bring it to a clean state.

## 1) Start with a rough file

Create a new file, for example `cnl/tutorial/demo.cnl`, with intentionally missing pieces:

```
To hello, produce Text
  Let x be 1.
  Return Text.concat("Hi ", name).
```

Notes:
- Missing module header
- Missing `:` at the end of the `To hello …` header
- Ambiguous numeric literal `1` (may affect overloads elsewhere)
- Potentially undefined `name` (we’ll focus on code actions in this tutorial)

## 2) Open in VS Code (or your LSP client)

Ensure the Aster LSP is running (via the bundled VS Code extension or a custom client pointing to `dist/src/lsp/server.js --stdio`).

Recommended settings (Settings → Aster Language Server):
- `format.mode`: `lossless`
- `format.reflow`: `true`
- `index.persist`: `true`
- `diagnostics.workspace`: `true`

## 3) Apply Quick Fixes

With the cursor on the first line:

- Missing module header
  - Use Quick Fix (Cmd/Ctrl+.) → “Fix: Add module header …”
  - The LSP infers a module name from the file path, for example `tutorial.demo`.

- Missing punctuation
  - When a diagnostic reports “Expected ':' or '.' at end of line”, use Quick Fix → “Fix: add ':' at end of line”.
  - Result header: `To hello, produce Text:`

- Disambiguate numeric overloads
  - Select the region containing `1` and trigger Quick Fix → “Fix: Disambiguate numeric overloads in selection”.
  - The LSP updates ambiguous numerics to explicit `1L` or `1.0` when relevant.

If you reference capabilities or interop helpers, additional Quick Fixes appear:

- Capability headers
  - If diagnostics indicate an IO/CPU effect mismatch, use Quick Fix → “Add It performs IO/CPU …”.
  - With `ASTER_CAPS` set to a JSON manifest, the LSP can also update the manifest to allow specific functions or modules.

## 4) Use Links and Hints

- Inlay hints
  - Enable inlay hints in your editor to see inferred types at `let` statements, parameter type hints, and function return type hints.

- Document links
  - Clicking the module name in `This module is …` can jump to the module file (if indexed).
  - `Text.*` references link to the Interop Overloads guide explaining overload resolution and nullability policies.

## 5) Final result (example)

After applying fixes, a cleaned version might look like:

```
This module is tutorial.demo.

To hello, produce Text:
  Let x be 1.
  Return Text.concat("Hi ", name).
```

You can now use additional features:
- Format document (lossless or normalize)
- Go to definition, find references, rename
- Workspace diagnostics and symbols via the persisted index

## See also

- Code Actions overview: docs/guide/lsp-code-actions.md
- Interop Overloads: docs/guide/interop-overloads.md
- Formatting and LSP setup: docs/guide/formatting.md

