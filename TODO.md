# TODOs: Interop Overloads + CI Promotion

- [ ] Core IR: add literal nodes for `Long` and `Double` (e.g., `LongE`, `DoubleE`) and corresponding type names in `Core.TypeName` (e.g., `Long`, `Double`). Wire them through lowering and pretty printer.
- [ ] Emitter: extend dotted static-call overload mapping to detect `Long` (`J`) and `Double` (`D`) from literal nodes and known locals; prefer primitive overloads first.
- [ ] Demo fixture: add `cnl/examples/interop_sum_core.json` (or CNL once literals exist) with:
  - `sumInt`: `Interop.sum(1, 2)` → expect `(II)Ljava/lang/String;`
  - `sumLong`: `Interop.sum(1L, 2L)` → expect `(JJ)Ljava/lang/String;`
  - `sumDouble`: `Interop.sum(1.0, 2.0)` → expect `(DD)Ljava/lang/String;`
- [ ] Javap assertion: add `scripts/javap-assert-sum.ts` to check the three signatures and wire an npm script `verify:asm:sum` (non‑blocking initially).
- [ ] CI promotion: after repeated green runs, promote javap interop assertion steps to blocking by removing non‑blocking wrappers in the `ci` script.

Nice-to-haves
- [ ] Consider a minimal overload resolution matrix: arity → exact primitive → primitive widening → boxing → varargs; emit a diagnostic when ambiguous.
- [ ] Expand fuzz verification to include basic static-call sites with known interop helpers.

---

# Additional Tasks from OPTIMISE.md

Build & Scripts Hygiene
- [ ] Remove any remaining references in docs to `ts-node` loader; ensure all examples use `dist/scripts/*.js`.

Emitter Quality
- [ ] Constant pool determinism/deduplication (determinism gate is present; implement CP interning/dedup where applicable).
- [ ] Add `lookupswitch` lowering path for sparse int matches (when Core introduces int-pattern matches).

Interop Hardening
- [ ] Formalize overload resolution policy (arity → exact → primitive widening → boxing → varargs) with clear tie-breaking.
- [ ] Nullability defaults/overrides for interop surfaces.
- [ ] Lightweight classpath scanning and cache to `build/.asteri`.

Effects & Capabilities
- [ ] Capability manifest (YAML/JSON) + compile-time checks mapping effects to declared capabilities.
- [ ] LSP code actions for inserting effects/capabilities.

Debuggability & Provenance
- [ ] Attach `(file,start,end)` to IR nodes; emit runtime `@AsterOrigin` and include spans in logs.

LSP & Formatter
- [ ] Lossless CST and idempotent formatter fuzzing.
- [ ] LSP features: hover/types/effects, go-to-def, find-refs, rename, semantic tokens, quick-fixes; p50 < 30ms on 100 files.

Tests & CI
- [ ] Promote javap interop assertions to blocking after N green runs.
- [ ] Expand verifier fuzz corpus (random CFGs, simple static calls) and keep non-blocking until stable.
- [ ] Native lane: keep non-blocking; document Xcode/GraalVM prerequisites.

Examples
- [ ] Add CI assertion that examples compile/run against package map.
