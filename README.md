# Aster Language

[![CI](https://github.com/wontlost-ltd/aster-lang/actions/workflows/ci.yml/badge.svg)](https://github.com/wontlost-ltd/aster-lang/actions/workflows/ci.yml)
[![Docs](https://github.com/wontlost-ltd/aster-lang/actions/workflows/docs.yml/badge.svg)](https://github.com/wontlost-ltd/aster-lang/actions/workflows/docs.yml)
[![Release Flow](https://github.com/wontlost-ltd/aster-lang/actions/workflows/release.yml/badge.svg)](https://github.com/wontlost-ltd/aster-lang/actions/workflows/release.yml)
[![GitHub Releases](https://github.com/wontlost-ltd/aster-lang/actions/workflows/github-release.yml/badge.svg)](https://github.com/wontlost-ltd/aster-lang/actions/workflows/github-release.yml)
[![Latest Release](https://img.shields.io/github/v/release/wontlost-ltd/aster-lang?display_name=tag)](https://github.com/wontlost-ltd/aster-lang/releases)
[![GitHub Stars](https://img.shields.io/github/stars/wontlost-ltd/aster-lang?style=social)](https://github.com/wontlost-ltd/aster-lang)
[![Node >= 22](https://img.shields.io/badge/node-%3E%3D22.0.0-339933?logo=node.js)](#installation--requirements)
[![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk)](#installation--requirements)
[![TypeScript 5.x](https://img.shields.io/badge/TypeScript-5.x-3178C6?logo=typescript)](#development)
[![Changesets](https://img.shields.io/badge/changesets-enabled-000000.svg?logo=changesets)](#release--versioning)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Null Strict Smoke](https://img.shields.io/badge/strict%20null-smoke-blue)](#interop-nullability-strict-mode--overrides)

Aster is a pragmatic, safe, and fast programming language with a human‑readable Controlled Natural Language (CNL) surface that lowers to a small, strict Core IR and targets the JVM. The repository contains the full TypeScript frontend (canonicalizer → lexer → parser → Core IR → typechecking) plus JVM emission paths, an LSP server, and Gradle‑based demos (ASM emitter, Truffle skeleton, runnable examples).

> Status: experimental preview. Interfaces and syntax may evolve.

## Highlights

- Human‑readable CNL with deterministic semantics
- Algebraic data types, pattern matching, effect annotations (IO/CPU)
- Non‑null by default; explicit Maybe/Option and Result
- Clean pipeline: canonicalize → lex → parse → lower to Core IR → emit
- JVM backends: Java source emission and direct bytecode via ASM
- LSP foundation for editor integration
- JVM interop overloads with primitive widening/boxing and reflective tie‑breaks. See Guide: JVM Interop Overloads (docs/guide/interop-overloads.md) for policy and disambiguation tips (use `1L` or `1.0`).
 - Interop nullability policy with LSP warnings and strict mode. See guide for defaults and overrides.

## Quick Demo

```text
To greet user: maybe User, produce Text:
  Match user:
    When null, Return "Hi, guest".
    When User(id, name), Return "Welcome, {name}".
```

```bash
# Build the TypeScript frontend and PEG grammar
npm run build

# Parse CNL → AST (JSON)
node dist/scripts/cli.js cnl/examples/greet.cnl

# Lower to Core IR (JSON)
node dist/scripts/emit-core.js cnl/examples/greet.cnl
```

## Installation & Requirements

- Node.js 22+ and npm
- Java 21+ (required for Gradle modules, JVM demos, and ASM emitter)
- macOS/Linux recommended

Install dependencies and build:

```bash
npm ci   # or: npm install
npm run build
```

## CLI Usage

Binaries (after build) are available under `node dist/...` and also mapped via `bin` when installed:

- `aster`: parse CNL → AST
- `aster-core`: lower to Core IR
- `aster-jvm`: emit Java sources
- `aster-lsp`: Language Server Protocol entrypoint

Examples:

```bash
# Parse to AST
node dist/scripts/cli.js cnl/examples/greet.cnl

# Emit Core IR
node dist/scripts/emit-core.js cnl/examples/greet.cnl

# Emit Java sources to build/jvm-src
node dist/scripts/emit-jvm.js cnl/examples/greet.cnl

# Run Core IR on Truffle (auto-lower .cnl)
node dist/scripts/aster.js truffle cnl/examples/if_param.cnl -- true
```

Truffle can also run an existing Core IR JSON:

```
node dist/scripts/aster.js truffle build/if_param_core.json -- false
```

## JVM Targets

Two paths are available:

- Java source emission (TypeScript → Java): `emit-jvm.js` writes `.java` files to `build/jvm-src` from Core IR.
- Direct JVM bytecode (ASM emitter): `emit-classfiles.js` builds and runs the Gradle module `aster-asm-emitter`, reading Core IR JSON on stdin and writing `.class` files to `build/jvm-classes`.

Typical flow to produce a runnable JAR for examples:

```bash
# Generate class files from a CNL program
node dist/scripts/emit-classfiles.js cnl/examples/greet.cnl

# Create a jar from emitted classes
node dist/scripts/jar-jvm.js

# Or run the end-to-end example workflows
npm run login:jar   # emit classes for login.cnl and jar them
npm run login:run   # run Java example using generated classes
```

For native demos (GraalVM native-image), see `examples/*-native` and the `native:hello` script. Ensure `JAVA_HOME` points to a JDK 21 toolchain.

ASM validation (classfiles):

```
npm run verify:asm
```

This emits classes for a couple of examples and runs `javap -v` to inspect the bytecode.

Interop strict nullability (non-blocking CI smoke):

```
# Demonstrates strict failure when passing null to a non-null interop param
npm run verify:asm:nullstrict   # see cnl/examples/null_strict_core.json
```

### Lambda Syntax & Verification

Lambda functions are supported in two CNL forms:

- Block form:
  - `Let f be function with x: Text, produce Text:` then an indented block.
- Short form:
  - `Let g be (y: Text) => Text.concat("2", y).`

See the Lambdas reference for details: docs/reference/lambdas.md

To verify ASM output for lambda examples:

```
# From Core JSON lambda fixtures
npm run verify:asm:lambda

# From CNL lambda examples (parse → lower → emit ASM → javap)
npm run verify:asm:lambda:cnl
```

## Language Server (LSP)

## Truffle Runner

- Use the unified CLI to run Core IR on the Truffle interpreter.

Examples:

```
# Auto-lower CNL to Core and run with arg(s)
node dist/scripts/aster.js truffle cnl/examples/if_param.cnl -- true

# Run an existing Core JSON with arg(s)
node dist/scripts/aster.js truffle build/if_param_core.json -- false
```

Notes:
- Extra values after `--` bind to function parameters as strings.
- Current Truffle coverage is a small subset (literals, names, let, if, return, and a few calls).

A minimal LSP server is included for experimentation:

```bash
npm run build
node dist/src/lsp/server.js --stdio
```

## Native Image (GraalVM)

The project targets a reflection‑free design. A small native sample exists under `examples/hello-native`.

- Local native build (requires GraalVM + Xcode toolchain on macOS):
  - `npm run native:hello`
- Lenient CI/native check (won’t fail if toolchain is missing):
  - `npm run native:hello:lenient`

See `docs/reference/native.md` for details and the reflection‑free policy.

A smoke test exists in `scripts/lsp-smoke.ts`. Editor integration is in progress; capabilities will expand over time.

## Using as a Library (ESM)

You can import the frontend as an ESM library inside Node 22+ environments:

```ts
import { canonicalize, lex, parse, lowerModule } from '@wontlost-ltd/aster-lang';

const src = `This module is app. To id, produce Int: Return 1.`;
const ast = parse(lex(canonicalize(src)));
const core = lowerModule(ast);
console.log(core);
```

Note: The npm package is not intended for public distribution during early development; see the Release section for current policy.

## Project Layout

- `src/` — TypeScript compiler pipeline (canonicalizer, lexer, parser, Core IR, JVM emitter, LSP)
- `scripts/` — build/test utilities (PEG build, golden, emit/jar, REPL, LSP smoke) compiled to `dist/scripts`
- `test/` — property, fuzz, and benchmark tests
- `cnl/examples/` — sample programs and golden fixtures
- `docs/` — VitePress site (API docs via TypeDoc); `dist/` — build output
- Gradle modules: `aster-asm-emitter/`, `truffle/`, `examples/*` (Java 21 toolchain)

## Development

Common tasks:

- Build: `npm run build`
- Dev/watch: `npm run dev`
- Type check: `npm run typecheck`
- Lint/format: `npm run lint`, `npm run lint:fix`, `npm run format`
- REPL: `node dist/scripts/repl.js`

Coding style:

- Strict TypeScript; explicit returns; avoid `any`
- ESM only; prefer `const` and pure modules
- Prettier: 2 spaces, single quotes, trailing commas, 100‑char width

## Testing

- All tests: `npm test`
- Property tests: `npm run test:property`
- Fuzz tests: `npm run test:fuzz`
- Benchmarks: `npm run bench`
- Golden tests: `npm run test:golden`
- Update goldens: `npm run test:golden:update` (review diffs before committing)

## Examples

Example CNL programs live in `cnl/examples`. JVM demo projects live under `examples/*` and assume generated classes are placed in `build/jvm-classes`:

```bash
# Arithmetic example end-to-end
./gradlew :aster-asm-emitter:run --args=build/jvm-classes < cnl/examples/arith_compare_core.json
npm run math:jar && ./gradlew :examples:math-jvm:run

# Text demo (interop mappings)
npm run text:run

# List demo (ASM interop for length/get/isEmpty)
npm run list:run

# Map demo (ASM interop for get)
npm run map:run
```

## Release & Versioning

The repository uses Changesets for versioning. Publishing to the public npm registry is disabled in CI. The current CI setup:

- On pushes to `main`, a Changesets action opens a version PR (no npm publish).
- To cut a GitHub Release without npm publish, push a tag like `v0.2.1`, or trigger the “GitHub Release” workflow manually in Actions.

Local packaging (no publish):

```bash
# Inspect what would be published without contacting npm
npm pack --dry-run
```

## Roadmap

This roadmap outlines the near‑term priorities and medium‑term goals. Timelines are indicative and may shift as the design evolves.

- Language and Semantics
  - Expand pattern matching: guards, nested destructuring, exhaustiveness checks
  - Modules and packages: imports, visibility, namespacing, package layout
  - Parametric polymorphism (generics) and improved type inference
  - Effects and capabilities: effect rows, async semantics, resource handling
  - Standard library: text, collections, result/maybe ops, IO primitives

- Compiler and IR
  - Core IR enrichment: explicit nullability, ownership/linearity experiments
  - Optimization passes: constant folding, dead‑code elimination, inlining where safe
  - Diagnostics: richer error reporting, suggestions, code frames across stages

- Backends and Runtime
  - JVM Java emitter: interop surface, exceptions mapping, collections bridge
  - JVM ASM emitter: broader instruction coverage, switch/lambda patterns, tests
  - Truffle/Graal: executable interpreter for Core IR, language context and nodes
  - Native pathways: refine demos via native‑image; FFI exploration

- Tooling and DX
  - LSP features: completion, hover, go‑to def, references, rename, formatting
  - Docs: language reference, semantics guide, backend and IR specification
  - CLI ergonomics: project scaffolding, config files, watch mode integrations
  - CI hardening: reproducible builds, hermetic tests, performance budgets

If you’re interested in a specific area, please open an issue or discussion.

## Contributing

Please read `CONTRIBUTING.md` for workflow, coding style, and guidelines. Run `npm run ci` (typecheck, lint, tests, build, smoke checks) before opening a PR.

## License

MIT © Aster Language Team

## Acknowledgements

- ASM (OW2) for bytecode generation
- VitePress and TypeDoc for docs
- GraalVM/Truffle APIs for runtime experimentation
## Interop Nullability (Strict Mode & Overrides)

- Enable strict emission failure on null passed to a non‑null interop parameter:

```
INTEROP_NULL_STRICT=true npm run verify:asm:nullstrict
```

- Provide a custom policy file to override defaults:

```
INTEROP_NULL_POLICY=$PWD/docs/examples/interop-null-policy.json npm run verify:asm:interop
```

See docs/guide/interop-overloads.md for details.
