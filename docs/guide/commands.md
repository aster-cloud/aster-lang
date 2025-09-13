# Commands Reference

This page lists common npm scripts and the unified `aster` CLI subcommands.

## Unified CLI (`aster`)

- `aster parse <file.cnl> [--watch]`
  - Parse CNL to AST (JSON). `--watch` re-parses on change.
- `aster core <file.cnl> [--watch]`
  - Lower to Core IR (JSON). `--watch` re-lowers on change.
- `aster jvm <file.cnl> [--out DIR] [--watch]`
  - Emit Java sources into `DIR` (default `build/jvm-src`).
- `aster class <file.cnl> [--out DIR] [--watch]`
  - Emit JVM classfiles (ASM) into `DIR` (default `build/jvm-classes`).
- `aster jar [<file.cnl>] [--out FILE]`
  - Create a jar from `build/jvm-classes`; if a CNL file is provided, emits classes first.
- `aster truffle <file.(cnl|json)> [-- args...]`
  - Run Core IR on the Truffle interpreter. Auto-lowers `.cnl` to JSON.
  - Extra args after `--` bind to function parameters as strings.

## Build & Typecheck

- `npm run build` — TypeScript build + PEG generation.
- `npm run dev` — Watch mode for TypeScript.
- `npm run typecheck` — TS typechecking (`--noEmit`).

## Tests & Benchmarks

- `npm test` — Goldens + property tests.
- `npm run test:golden` — Golden tests only.
- `npm run test:property` — Property tests.
- `npm run test:fuzz` — Fuzz tests.
- `npm run bench` — Benchmarks.

## Lint & Format

- `npm run lint` — ESLint.
- `npm run lint:fix` — ESLint with fixes.
- `npm run format` — Prettier write.
- `npm run format:check` — Prettier check.

## Docs

- `npm run docs:dev` — VitePress dev server.
- `npm run docs:build` — Build docs site.
- `npm run docs:preview` — Preview built docs.
- `npm run docs:api` — Generate API docs via TypeDoc.
- `npm run docs:all` — API docs + site build.

## JVM/ASM Emit & Verify

- `npm run emit:class` — Emit classfiles (reads `build/last-core.json` or stdin internally).
- `npm run javac:jvm` — Compile emitted Java sources into classfiles.
- `npm run jar:jvm` — Jar emitted classes to `build/aster-out/aster.jar`.
- `npm run verify:asm` — Build, emit classfiles for several examples, and run `javap -v` across them.

## Examples

- `npm run login:run` — Build/login demo on JVM.
- `npm run math:run` — Math demo on JVM.
- `npm run text:run` — Text interop demo (concat/startsWith/indexOf).
- `npm run list:run` — List interop demo (length/get/isEmpty).
- `npm run map:run` — Map interop demo (get).

## Truffle

- `npm run truffle:run` — Run Truffle demo runner.
- `npm run truffle:run:core` — Lower `greet.cnl` and run on Truffle.
- `npm run truffle:run:ifparam` — Lower `if_param.cnl` and run on Truffle with a flag.

## Misc

- `npm run repl` — Experimental REPL.
- `npm run lsp` — Start the LSP server (stdio).
- `npm run new -- <dir>` — Scaffold a new Aster project in `<dir>`.

