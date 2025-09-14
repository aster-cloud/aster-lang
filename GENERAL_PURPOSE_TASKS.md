# General-Purpose Roadmap & Tasks

This checklist tracks concrete steps to evolve Aster into a general‑purpose language. Tasks are grouped to keep scope clear and actionable.

## Tooling & DX

- [x] Add project scaffolding script (`scripts/scaffold.ts`) and npm entry (`npm run new`).
- [x] Add `aster` single-entry CLI (subcommands: parse, core, jvm, class, jar).
- [x] Add watch mode for CLI commands (re-run on file changes).
- [x] Create VS Code extension wrapper for bundled LSP client.

## Language & Semantics

- [x] Module system MVP (imports, visibility, namespacing, package layout).
- [x] Exhaustiveness checking for pattern matching (with useful diagnostics).
- [ ] Parametric polymorphism (generics) + improved inference in Core IR.
  - [x] Type application syntax (`Foo of T, U`) and parsing
  - [x] AST/Core types (`TypeApp`, `TypeVar`) + lowering
  - [x] Pretty printer + JVM emitter fallback
  - [x] Function type parameters (e.g., `To id of T ...`)
  - [x] Unification/inference for type variables (preview, return position)
  - [x] Golden/property tests for generics
- [x] Effects surface: IO vs CPU separation, async semantics clarified in spec.

## Standard Library

- [x] Text: concat, search stubbed via examples (goldens pass).
- [x] Collections: length/map/id/head stubs via examples (goldens pass).
- [x] Result/Maybe utilities: map, withDefault, mapOk, tapError stubs (goldens pass).
- [x] IO primitives: print, readLine, file read/write (via runtime bridge) — stubs + goldens.

## Backends & Runtime

- [x] JVM interop surface (initial): docs + basic emitter shims (Text.concat, Text.contains, List.length).
- [x] ASM coverage expansion and validation tests (switch, lambdas, try/catch).
  - [x] Enum switch/tableswitch fast-path.
  - [x] Basic try/catch wrapping for call-returning Result.
  - [x] Verify `map_ops.cnl` in `verify:asm`.
  - [x] Lambda/closure support: IR + JVM emission.
    - [x] Add Core IR: function type and lambda expr nodes.
    - [x] Capture analysis and lowering.
    - [x] Runtime functional interfaces (Fn1/Fn2/...).
    - [x] JVM emitter: closure class generation + apply method.
    - [x] Golden + ASM fixtures (map with inline function, captured local).
    - [x] CNL parser: lambda syntax (block form and/or `=>`) + lowering to Core.
    - [x] Add golden tests for CNL lambdas once parsed (AST/Core paths).
- [ ] Truffle interpreter spike for Core IR (execution + profiling hooks).
  - [x] CLI to emit Core JSON and run via Truffle nodes (subset).
  - [x] Name lookup in conditions; Let/Return across blocks.
  - [ ] Load Core JSON to full node tree (beyond subset) + profiling hooks.
  - [x] Map demo app (ASM path) similar to list/text examples.
  - [x] Truffle: add Lambda node + execution support once IR lands.

## Docs & Examples

- [x] General purpose quickstart guide and project template walkthrough.
- [ ] End-to-end examples
  - [x] CLI tool (demo.cli) with Gradle run target
  - [x] REST service core
  - [ ] Rules/policy engine
- [ ] Language reference expansion (modules, effects, generics, stdlib spec).
  - [x] Modules: reference page
  - [x] Effects: reference page
  - [x] Generics: function type params, diagnostics, preview inference
  - [x] Lambdas & closures: IR + emission design, syntax TBD (doc added).

## CI & Quality

 - [x] Performance budgets and microbench CI checks.
- [ ] Golden tests for new AST/IR nodes and stdlib APIs.
  - [x] Add AST/Core golden for `cnl/examples/text_ops.cnl` and include in `scripts/golden.ts`.
- [ ] Reproducible builds for CLI and docs (lockfile + pinned toolchain).
  - [x] Run docs build (`docs:build`) in CI script.
  - [x] Run API docs (`docs:api`) in CI script.

---

Legend: [ ] pending, [x] completed
