---
title: LSP Code Actions
---

# LSP Code Actions (Quick Fixes)

The Aster language server provides a small but useful set of Quick Fixes to speed up editing:

- Disambiguate numeric overloads
  - Converts ambiguous numeric literals (e.g., `1`) to explicit types in a selection: `1L` (Long) or `1.0` (Double).
  - Helpful for JVM overloads; see the Interop Overloads guide.

- Insert/remove capability headers
  - Adds or removes “It performs IO/CPU” on `To …` lines when diagnostics indicate a capability/effect mismatch.
  - With `ASTER_CAPS` set to a capabilities manifest, the server also offers edits to update the manifest (allow function or module).

- Add missing module header
  - Inserts `This module is <name>.` at the top of the file using an inferred module path.
  - Enables better cross‑file features and indexing across sessions.

- Add missing punctuation
  - Inserts `:` or `.` at the end of a line if required by syntax (based on parser diagnostics).

## Tips

- Workspace diagnostics can be toggled via `asterLanguageServer.diagnostics.workspace`.
- Persistent index (`.asteri/lsp-index.json`) improves cross‑file find‑refs/rename without opening files.
- Lossless formatting preserves comments and whitespace while applying minimal fixes when enabled.

See also:

- Interop Overloads: docs/guide/interop-overloads.md
- Formatting and LSP setup: docs/guide/formatting.md

