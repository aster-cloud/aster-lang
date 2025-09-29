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
    - [ ] Persisted workspace symbol/index across sessions (disk-backed cache) for cross-file features without opening files.
    - [ ] Broader cross-file rename/refs across the repo (scan-once with index), not limited to open documents.
    - [ ] Optional: CST-aware inline comment preservation in formatter output (behind a flag).

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
- [ ] Ensure CST captures full trivia
  - [ ] Extend `src/cst_builder.ts` to attach leading/trailing trivia to every token (whitespace and comments) and keep exact token text/spans.
  - [ ] Verify lexer exposes enough information (token boundaries) to slice trivia precisely.
- [ ] Implement printer
  - [ ] Add `src/cst_printer.ts` with `printCNLFromCst(cst: CstModule, opts?): string` that emits:
    - token.leadingTrivia + token.text + token.trailingTrivia for all tokens in order
    - preserves BOM and trailing newline as in original
  - [ ] Unit tests for identity on valid inputs (no changes expected).
- [ ] Wire into formatter (non‑breaking)
  - [ ] Update `formatCNL` to try CST print first when `mode === 'lossless'`; fallback to current AST path when parse fails.
  - [ ] Keep existing behavior when `mode` is omitted (defaults to normalize).

Phase 2 — Minimal Reflow Rules (Lossless Mode Only)
- [ ] Add tiny seam fixes guarded by options:
  - [ ] Collapse `. :` → `:`, ` .` → `.`, and remove spaces before `.,:!?;`.
  - [ ] Ensure at most one trailing newline; preserve if present.
  - [ ] Never change indentation width or tabs; preserve as found.
- [ ] Tests:
  - [ ] Golden cases for each seam fix; ensure no other bytes change.

Phase 3 — Configuration & CLI
- [ ] `formatCNL` options
  - [ ] Support `{ mode: 'lossless' | 'normalize' }` and plumb through callers.
- [ ] CLI support
  - [ ] Update `dist/scripts/format-examples.js` to parse `--lossless` and pass mode.
  - [ ] Add `npm` scripts: `fmt:examples:lossless`, `fmt:examples:check:lossless`.
  - [ ] README: document lossless vs normalize modes and when to use each.

Phase 4 — Tests & CI
- [ ] Idempotency fuzz
  - [ ] Add a fuzz that injects random trivia/comments around tokens, builds CST, prints, and asserts identity.
- [ ] Golden pairs
  - [ ] Add a few “ugly but valid” inputs under `test/golden-lossless` and assert byte‑for‑byte equality.
- [ ] CI
  - [ ] Add a non‑blocking job to run lossless tests; promote to blocking after green runs.

Phase 5 — LSP Integration (Optional)
- [ ] Add LSP range formatting that reprints only the requested slice from CST.
- [ ] Expose a server setting to choose lossless vs normalize.

Acceptance Criteria
- [ ] For valid inputs, `mode: 'lossless'` emits identical bytes (modulo optional seam fixes when explicitly enabled).
- [ ] For invalid inputs, `formatCNL` gracefully falls back to current normalize path without crashing.
- [ ] `fmt:examples` remains deterministic (normalize mode) and `--lossless` is available for user edits.
