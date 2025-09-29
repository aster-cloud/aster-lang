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
  - [ ] Mixed int + default: refactor lowering to inline default body and avoid shared end labels; re‑enable CI assertion after stabilization.

Interop Hardening
- [x] Formalize overload resolution policy (arity → exact → primitive widening → boxing → varargs) with clear tie-breaking.
  - Implemented heuristic + scoring + varargs in emitter; added docs/guide/interop-overloads.md.
- [x] Nullability defaults/overrides for interop surfaces.
  - Emitter warnings and strict mode (INTEROP_NULL_STRICT); LSP diagnostics and quick‑fixes; policy override via INTEROP_NULL_POLICY.
- [x] Lightweight classpath scanning and cache to `build/.asteri`.
  - Added method cache (method-cache.json) + fallback selection; scripts to inspect/clear/seed.
- [x] Map core `Number` to JVM `java.lang.Double`; add runtime boxing helpers `aster.runtime.Primitives` (number/integer/longNum/bool); update examples and docs to use helpers.

Effects & Capabilities
- [ ] Capability manifest (YAML/JSON) + compile-time checks mapping effects to declared capabilities.
- [ ] LSP code actions for inserting effects/capabilities.

Debuggability & Provenance
- [ ] Attach `(file,start,end)` to IR nodes; emit runtime `@AsterOrigin` and include spans in logs.

LSP & Formatter
- [ ] Lossless CST and idempotent formatter fuzzing.
- [ ] LSP features: hover/types/effects, go-to-def, find-refs, rename, semantic tokens, quick-fixes; p50 < 30ms on 100 files.

Tests & CI
- [x] Promote javap interop assertions to blocking after N green runs.
- [x] Expand verifier fuzz corpus (random CFGs, simple static calls) and keep non-blocking until stable.
- [ ] Native lane: keep non-blocking; document Xcode/GraalVM prerequisites.

Examples
- [x] Add CI assertion that examples compile against package map (verify:examples is blocking in CI).
