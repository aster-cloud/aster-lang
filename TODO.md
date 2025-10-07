⚠️ Archived — see `.claude/archive/TODO-legacy.md`

# TODOs: Interop Overloads + CI Promotion

- [x] Core IR: add literal nodes for `Long` and `Double` (e.g., `LongE`, `DoubleE`) and corresponding type names in `Core.TypeName` (e.g., `Long`, `Double`). Wire them through lowering and pretty printer.
  - [x] Core JSON + emitter + TS Core types added; pretty printer supports Long/Double.
  - [x] CNL surface syntax and lowering: parser + AST support for long/double literals and wired through `lower_to_core`.
- [x] Emitter: extend dotted static-call overload mapping to detect `Long` (`J`) and `Double` (`D`) from literal nodes and locals; prefer primitive overloads first.
- [x] Demo fixture: add `cnl/examples/interop_sum_core.json` (or CNL once literals exist) with:
  - `sumInt`: `Interop.sum(1, 2)` → expect `(II)Ljava/lang/String;`
  - `sumLong`: `Interop.sum(1L, 2L)` → expect `(JJ)Ljava/lang/String;`
  - `sumDouble`: `Interop.sum(1.0, 2.0)` → expect `(DD)Ljava/lang/String;`
  - mixed: `Interop.sum(1, 2L)` → `(JJ)`, `Interop.sum(1, 2.0)` → `(DD)`
- [x] Javap assertion: add `scripts/javap-assert-sum.ts` to check the signatures and wire `verify:asm:sum` (non‑blocking initially).
- [x] CI promotion: after repeated green runs, promote javap interop assertion steps to blocking by removing non‑blocking wrappers in the `ci` script.

Nice-to-haves
- [x] Consider a minimal overload resolution matrix: arity → exact primitive → primitive widening → boxing → varargs; emit a diagnostic when ambiguous.
  - Implemented heuristic + reflective tie‑breaks; added emitter + TS/LSP diagnostics and code actions.
- [x] Expand fuzz verification to include basic static-call sites with known interop helpers.

---

# Additional Tasks from OPTIMISE.md

Build & Scripts Hygiene
- [x] Remove any remaining references in docs to `ts-node` loader; ensure all examples use `dist/scripts/*.js`.
- [x] Make `scripts/emit-classfiles` accept multiple `.cnl` inputs and emit into a single `build/jvm-classes` directory.
- [x] Ensure `examples/login-jvm` emits all required modules (login.cnl, policy_engine.cnl, policy_demo.cnl) before jarring.

Emitter Quality
- [x] Constant pool determinism/deduplication (determinism gate is present; implement CP interning/dedup where applicable).
  - Done: stabilized iteration order for decls/fields/variants, deterministic line numbering, deterministic method-cache fallback; added string constant pooling in emitter; ASM writer already de‑dups constants. Determinism check `determinism:asm` passes.
  - [x] Add switch lowering for int-pattern matches (tableswitch/lookupswitch) when all cases are integer literals; verified via javap.
  - [x] Mixed int + default: refactor lowering to inline default body and avoid shared end labels; re‑enable CI assertion after stabilization.

Interop Hardening
- [x] Formalize overload resolution policy (arity → exact → primitive widening → boxing → varargs) with clear tie-breaking.
  - Implemented heuristic + scoring + varargs in emitter; added docs/guide/interop-overloads.md.
- [x] Nullability defaults/overrides for interop surfaces.
  - Emitter warnings and strict mode (INTEROP_NULL_STRICT); LSP diagnostics and quick‑fixes; policy override via INTEROP_NULL_POLICY.
- [x] Lightweight classpath scanning and cache to `build/.asteri`.
  - Added method cache (method-cache.json) + fallback selection; scripts to inspect/clear/seed.
- [x] Map core `Number` to JVM `java.lang.Double`; add runtime boxing helpers `aster.runtime.Primitives` (number/integer/longNum/bool); update examples and docs to use helpers.

Effects & Capabilities
- [x] Capability manifest (YAML/JSON) + compile-time checks mapping effects to declared capabilities.
- [x] LSP code actions for inserting effects/capabilities.

Debuggability & Provenance
- [x] Attach `(file,start,end)` to IR nodes; emit runtime `@AsterOrigin` and include spans in logs.
  - IR: `origin` propagated on all Core nodes in lowering (Module, decls, stmts, exprs, patterns, types).
  - Runtime: added `aster.runtime.AsterOrigin` (RUNTIME retention).
  - Emitter: annotates Data/Enum classes, function methods, and lambda classes + apply(); logs include spans.

LSP & Formatter
- [x] Idempotent formatter fuzzing.
- [x] Formatter prints block‑form lambdas under Let without stray periods.
- [x] Formatter prints Start/Wait and preserves strict headers (effects in header).
- [x] Formatter prints `none` (Maybe/Option) and no longer rewrites it to placeholders.
- [x] Example formatter (`fmt:examples`) sanitizes legacy placeholders (`<expr>.`) to strict forms when necessary.
  - [x] LSP features: hover/types/effects, go-to-def, find-refs, rename, semantic tokens, quick-fixes; p50 < 30ms on 100 files.
  - Follow-ups
    - [x] Persisted workspace symbol/index across sessions (disk-backed cache) for cross-file features without opening files.
      - [x] Design: JSON index per workspace root under `.asteri/lsp-index.json` keyed by module + decl name → spans/uris.
      - [x] Add indexer: CLI script `scripts/lsp-build-index.ts` to scan `cnl/**/*.cnl`, canonicalize+lex+parse and serialize minimal symbol info.
      - [x] Server integration: on open/save and via file-watcher, write `.asteri/lsp-index.json`; load on server init; settings `asterLanguageServer.index.persist` and `asterLanguageServer.index.path` supported.
    - [x] Adopt LSP 3.17 diagnostics
      - [x] Implement `textDocument/diagnostic` with full report; refactor push path into `computeDiagnostics`.
      - [x] Keep caches warm on edits; remove duplicate push calls.
      - [x] Preserve existing quick-fixes and semantic info.
      - [x] Enable workspace diagnostics and implement `workspace/diagnostic` over the persisted index + open docs.
    - [x] Inlay hints (initial)
      - [x] Literal type hints for Int/Long/Double/Text/Bool/null.
      - [x] Let-inferred type hints using `exprTypeText`.
    - [x] Additional code actions
      - [x] Add missing module header (infer from path).
      - [x] Add missing punctuation at end-of-line when parser expects ':' or '.'.
      - [x] Bulk numeric overload disambiguation for selection.
    - [x] Semantic tokens refinements (enum variants, fields; declaration modifier for decls).
    - [x] Document links (module header and dotted Module.member to module file when indexed; Text.* → interop guide).
      - [x] LSP server boot: onInitialize load cache (if same workspace hash), schedule background re-index if stale.
      - [x] Invalidation: listen to file change (didChangeWatchedFiles) and update changed entries; write-through after debounce.
      - [x] Config: `asterLanguageServer.index.persist` (default true), location override `asterLanguageServer.index.path`.
      - [x] Tests: build small fixture workspace, index load/save round-trip, symbol lookup without opening files (`test:lsp-index`).
      - [x] Health: custom request `aster/health` and CLI `npm run lsp:health` to inspect watcher capability and index size.
    - [x] Broader cross-file rename/refs across the repo (scan-once with index), not limited to open documents.
      - [x] Add workspace-wide reference finder using persisted index for candidate URIs + on-demand token scan to refine matches.
      - [x] Rename: build WorkspaceEdit across all candidate files, verify spans with token boundaries to avoid substring collisions.
    - [x] Streaming edits: chunk large workspaces, display progress (window/logMessage) and allow cancel.
      - [x] References: stream partial result chunks via $/progress; configurable chunk size (`asterLanguageServer.streaming.referencesChunk`).
      - [x] Rename: chunk processing with frequent progress reports; configurable chunk size (`asterLanguageServer.streaming.renameChunk`).
      - [x] Cancellation honored during long loops for both.
        - [x] Progress/cancel
        - [x] Chunking large workspaces
      - [x] Config: `asterLanguageServer.rename.scope: 'open' | 'workspace'` (default 'workspace').
    - [x] Tests: multi-file examples; ensure edits are correct and stable.
      - [x] Added fixture under `test/lsp-multi/` and a deterministic cross-file rename test `scripts/lsp-multi-rename.test.ts`.
      - [x] Wired into CI via `npm run test:lsp-multi`.
    - [x] Optional: CST-aware inline comment preservation in formatter output (behind a flag).
      - [x] Extend CST builder to collect line comments as structured trivia with (line, text, standalone) and surface via `inlineComments`.
      - [x] Add `formatCNL(text, { mode: 'normalize', preserveComments: true, preserveStandaloneComments?: boolean })` to re-emit inline and (optionally) standalone comments.
      - [x] Heuristics: reattach inline EOL comments to non-empty lines; insert standalone comments near header/block or at end.
      - [x] CLI: `format-examples.js` and `format-file.js` accept `--preserve-comments`.
    - [x] Tests: golden cases under `test/comments/golden` + runner `scripts/test-comments-golden.ts` (now in CI, blocking).

Comment Preservation (Precise Attachment) — Future Plan
- [ ] Capture attachment points
  - [ ] Extend CST to tag comments with nearest token/node role (e.g., after header token, inside block at line N)
  - [ ] Encode relative position: before/after token, column offset
- [ ] Map normalize output to CST nodes
  - [ ] During normalize formatting, emit node boundary metadata (header/body/stmt indices)
  - [ ] Build a lightweight map from formatted lines back to node roles
- [ ] Placement algorithm
  - [ ] For each comment, choose a destination line by matching its role and nearest node boundary
  - [ ] Preserve ordering of multiple comments targeting the same region
  - [ ] Fall back to current heuristics when role mapping is ambiguous
- [ ] Edge cases
  - [ ] Multi-line blocks with reflowed punctuation at boundaries (e.g., `. :`)
  - [ ] Consecutive standalone comment runs
  - [ ] Mixed inline and standalone comments on adjacent lines
- [ ] Configuration
  - [ ] `asterLanguageServer.format.preserveStandaloneStrategy`: 'top-bottom' | 'adjacent' | 'off' (default 'top-bottom')
  - [ ] CLI flags for choosing standalone placement strategy
- [ ] Additional Goldens
  - [ ] Examples covering match blocks, nested lambdas, and interop calls with comments
- [ ] Tests
  - [ ] Golden suites covering: header comments, inside nested blocks, after returns, mixed comment sequences
  - [ ] Property tests exercising role mapping stability under trivial content edits

Tests & CI
- [x] Promote javap interop assertions to blocking after N green runs.
- [x] Expand verifier fuzz corpus (random CFGs, simple static calls) and keep non-blocking until stable.
- [x] Native lane: keep non-blocking; document Xcode/GraalVM prerequisites.

Examples
- [x] Add CI assertion that examples compile against package map (verify:examples is blocking in CI).
- [x] Update all example CNL to strict grammar:
  - comma‑separated params, effect sentences in headers, Start/Wait blocks
  - operator calls in functional form (e.g., `<(x, y)`, `+(x, y)`, `>(a, b)`)
  - block‑form lambdas indented properly; no trailing double periods

Parser & Surface Syntax
- [x] Comma‑separated parameter lists (and trailing comma before `produce`).
- [x] Operator names as callable targets (`<`, `>`, `+`, `-`).
- [x] Allow single‑statement blocks after a colon without extra indent (strict mode still prefers indent).
- [x] Construction fields accept commas as well as `and`.

---

# Lossless CST Printing (Trivia‑Preserving Reflow)

Goals
- Preserve all user trivia (spaces, newlines, comments, BOM) while re‑emitting the same bytes for valid inputs.
- Provide a minimal reflow that only fixes punctuation seams (e.g., `. :` → `:`) without altering indentation or comments.
- Keep current strict formatter (“normalize” mode) for canonical outputs (used by `fmt:examples`).

Deliverables
- A new `printCNLFromCst(cst, opts)` that can emit text losslessly from the CST.
- `formatCNL(text, opts)` gains a `{ mode: 'lossless' | 'normalize' }` option.
- `format-examples.js` (`fmt:examples`) accepts `--lossless` and `--check --lossless`.
- Tests: idempotency fuzz, golden pairs of “ugly but valid” inputs, CI wiring.

Plan (Executable Steps)

Phase 1 — CST Byte‑Preserving Printer
- [x] Ensure CST captures full trivia
  - [x] Extend `src/cst_builder.ts` to preserve fullText and token offsets/spans for exact slicing.
  - [x] Verify lexer exposes enough information (token boundaries) to slice trivia precisely.
- [x] Implement printer
  - [x] Add `src/cst_printer.ts` with `printCNLFromCst(cst: CstModule, opts?)` (lossless) and `printRangeFromCst`.
  - [x] Preserve BOM and trailing newline as in original.
- [x] Wire into formatter (non‑breaking)
  - [x] Update `formatCNL` to try CST print first when `mode === 'lossless'`; fallback to current AST path when parse fails.
  - [x] Keep existing behavior when `mode` is omitted (defaults to normalize).

Phase 2 — Minimal Reflow Rules (Lossless Mode Only)
- [x] Add tiny seam fixes guarded by options:
  - [x] Collapse `. :` → `:`, ` .` → `.`, and remove spaces before `.,:!?;`.
  - [x] Ensure at most one trailing newline; preserve if present.
  - [x] Never change indentation width or tabs; preserve as found.
- [x] Tests:
  - [x] Lossless idempotency check across examples (`npm run test:lossless`).

Phase 3 — Configuration & CLI
- [x] `formatCNL` options
  - [x] Support `{ mode: 'lossless' | 'normalize', reflow?: boolean }` and plumb through callers.
- [x] CLI support
  - [x] Update `dist/scripts/format-examples.js` to parse `--lossless` and `--lossless-reflow`.
  - [x] Add `npm` scripts: `fmt:examples:lossless`, `fmt:examples:check:lossless`, `fmt:examples:lossless:reflow`, `fmt:examples:check:lossless:reflow`.
  - [x] README: document lossless vs normalize modes and when to use each.

Phase 4 — Tests & CI
- [x] Idempotency fuzz
  - [x] Add a fuzz that injects random trivia/comments around tokens, builds CST, prints, and asserts identity (`test/lossless.fuzz.test.ts`).
- [x] Golden pairs
  - [x] Added multiple “ugly but valid” inputs under `test/lossless/golden` and assert:
        (a) lossless preserves input, (b) lossless+reflow equals expected output.
- [x] CI
  - [x] Add a non‑blocking job to run lossless tests; promote to blocking after green runs.

Phase 5 — LSP Integration (Optional)
- [x] Add LSP range formatting that reprints only the requested slice from CST (with seam reflow).
- [x] Expose a server setting to choose lossless vs normalize and a `reflow` toggle.
- [x] Add full-document formatting using the same settings.

Docs
- [x] Add Formatting, LSP & CLI guide page and wire it into VitePress nav/sidebar.

Acceptance Criteria
- [x] For valid inputs, `mode: 'lossless'` emits identical bytes (modulo optional seam fixes when explicitly enabled).
- [x] For invalid inputs, `formatCNL` gracefully falls back to current normalize path without crashing.
- [x] `fmt:examples` remains deterministic (normalize mode) and `--lossless` is available for user edits.
