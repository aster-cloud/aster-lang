# Aster Language

[![CI](https://github.com/wontlost-ltd/aster-lang/actions/workflows/ci.yml/badge.svg)](https://github.com/wontlost-ltd/aster-lang/actions/workflows/ci.yml)
[![Docs](https://github.com/wontlost-ltd/aster-lang/actions/workflows/docs.yml/badge.svg)](https://github.com/wontlost-ltd/aster-lang/actions/workflows/docs.yml)
[![Release Flow](https://github.com/wontlost-ltd/aster-lang/actions/workflows/release.yml/badge.svg)](https://github.com/wontlost-ltd/aster-lang/actions/workflows/release.yml)
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
- Algebraic data types, pattern matching, effect annotations (IO/CPU) enforced at compile-time
- Non‑null by default; explicit Maybe/Option and Result
- Clean pipeline: canonicalize → lex → parse → lower to Core IR → emit
- JVM backends: Java source emission and direct bytecode via ASM
- LSP foundation for editor integration
- JVM interop overloads with primitive widening/boxing and reflective tie‑breaks. See Guide: JVM Interop Overloads (docs/guide/interop-overloads.md) for policy and disambiguation tips (use `1L` or `1.0`).
 - Interop nullability policy with LSP warnings and strict mode. See guide for defaults and overrides.

## Quick Demo

### 方式 1: 使用 Docker/Podman (最快体验)

```bash
# 使用 Podman 运行 Fibonacci 示例 (无需安装 Node.js/Java)
podman run --rm \
  -v $(pwd)/benchmarks:/benchmarks:ro \
  ghcr.io/wontlost-ltd/aster-truffle:latest \
  /benchmarks/core/fibonacci_20_core.json \
  --func=fibonacci -- 10

# 预期输出: 6765
# 启动时间: ~50ms (GraalVM Native Image)
# 镜像大小: 163 MB
```

### 方式 2: 从源码构建 (开发模式)

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
node dist/scripts/cli.js test/cnl/examples/greet.aster

# Lower to Core IR (JSON)
node dist/scripts/emit-core.js test/cnl/examples/greet.aster

# Run with Truffle interpreter
node dist/scripts/aster.js truffle test/cnl/examples/greet.aster --func=greet
```

## Installation & Requirements

### 快速体验 (推荐新手)

仅需 **Docker** 或 **Podman**:
```bash
# 拉取预构建镜像
podman pull ghcr.io/wontlost-ltd/aster-truffle:latest

# 或本地构建
podman build -f Dockerfile.truffle -t aster/truffle:latest .
```

### 完整开发环境

- **Node.js 22+** and npm
- **Java 25 LTS** (推荐) 或 **Java 21+** (最低要求)
  - 推荐: [GraalVM CE 25](https://www.graalvm.org/downloads/)
- macOS/Linux 推荐

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
node dist/scripts/cli.js test/cnl/examples/greet.aster

# Emit Core IR
node dist/scripts/emit-core.js test/cnl/examples/greet.aster

# Emit Java sources to build/jvm-src
node dist/scripts/emit-jvm.js test/cnl/examples/greet.aster

# Run Core IR on Truffle (auto-lower .aster)
node dist/scripts/aster.js truffle test/cnl/examples/if_param.aster -- true
```

Truffle can also run an existing Core IR JSON:

```
node dist/scripts/aster.js truffle build/if_param_core.json -- false
```

## Native 构建

- Native 支持现已集成 CLI，可通过 GraalVM Native Image 将编译器与用户程序打包为独立可执行文件。
- 环境要求：GraalVM JDK 25+，并使用 `gu install native-image` 安装原生工具链。
- 快速示例：

```bash
# 构建 CLI 原生可执行文件
./gradlew :aster-lang-cli:nativeCompile

# 将用户程序转换为原生二进制
aster native examples/cli-jvm/src/main/resources/hello.aster --output hello-native
```

- 查看完整操作手册与阶段设计，请参考 `docs/native-build-guide.md`。


## JVM Targets

Two paths are available:

- Java source emission (TypeScript → Java): `emit-jvm.js` writes `.java` files to `build/jvm-src` from Core IR.
- Direct JVM bytecode (ASM emitter): `emit-classfiles.js` builds and runs the Gradle module `aster-asm-emitter`, reading Core IR JSON on stdin and writing `.class` files to `build/jvm-classes`.

Typical flow to produce a runnable JAR for examples:

```bash
# Generate class files from a CNL program
node dist/scripts/emit-classfiles.js test/cnl/examples/greet.aster

# Create a jar from emitted classes
node dist/scripts/jar-jvm.js

# Or run the end-to-end example workflows
npm run login:jar   # emit classes for login.aster and jar them
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
npm run verify:asm:nullstrict   # see test/cnl/examples/null_strict_core.json
```

## Workflow 并发特性（Phase 2.4）

- 语言新增 `step foo depends on ["bar", "baz"]` 语法，编译器会在 Core IR 中写入显式依赖图；未声明依赖时自动回退为串行执行，兼容旧 Workflow。
- 运行时以 `AsyncTaskRegistry` + `CompletableFuture` + `ExecutorService` 调度就绪步骤，`WorkflowScheduler` 仅负责触发 `executeUntilComplete()` 并传播异常。
- `DependencyGraph.addTask` 自带 DFS 循环检测；调度期间若无就绪节点但仍有未完成任务，则抛出 `IllegalStateException("Deadlock detected")`，便于运行团队快速定位设计问题。
- 补偿逻辑使用 LIFO 栈记录完成顺序，即便在并发 fan-out/diamond 模式中也能按真实提交顺序撤销副作用。
- 示例位于 `quarkus-policy-api/src/main/resources/policies/examples/`：涵盖 fan-out、diamond、串行兼容三类模式，可直接用于演示或回归测试。

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

The repo includes a Node-based LSP server and a VS Code client.

- Server entry: `dist/src/lsp/server.js` (run with `--stdio`)
- VS Code client: see `aster-vscode`

Features
- Hover: types/effects, interop previews, return types
- Go to definition, find references, workspace symbols
- Rename (open docs; dotted rename across workspace), persisted index for closed files
- Diagnostics: pull (`textDocument/diagnostic`), optional workspace diagnostics
- Diagnostics severity levels: errors (e.g., missing effects), warnings, and info (e.g., @io declared but only CPU-like work).

- Formatting: lossless and normalize modes, range/document
- Quick fixes: numeric overload disambiguation, capability header edits (It performs IO/CPU), capability manifest updates, missing module header, punctuation fixes

Settings (VS Code → Aster Language Server)
- `asterLanguageServer.index.persist` (default true)
- `asterLanguageServer.index.path` (optional override)
- `asterLanguageServer.format.mode` (default `lossless`)
- `asterLanguageServer.format.reflow` (default true)
- `asterLanguageServer.rename.scope` (default `workspace`)
- `asterLanguageServer.diagnostics.workspace` (default true)

Tutorials and guides
- LSP Quick Fix Tutorial: docs/guide/lsp-tutorial.md
- LSP Code Actions overview: docs/guide/lsp-code-actions.md

## Truffle Runner

- Use the unified CLI to run Core IR on the Truffle interpreter.

Examples:

```
# Auto-lower CNL to Core and run with arg(s)
node dist/scripts/aster.js truffle test/cnl/examples/if_param.aster -- true

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
- `test/cnl/examples/` — sample programs and golden fixtures
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
- Example formatting:
  - Strict normalize (CI default): `npm run fmt:examples` rewrites examples to strict CNL
    (effects in headers, comma‑separated params) and sanitizes legacy placeholders.
  - Lossless (preserve trivia): `npm run fmt:examples:lossless` prints byte‑for‑byte
    using the CST (use `:check` variants to verify without writing). Add `--lossless-reflow`
    via `fmt:examples:lossless:reflow` for minimal seam fixes (e.g., `. :` → `:`).
  - Preserve inline comments (normalize): add `--preserve-comments` to keep end‑of‑line comments on corresponding lines (best effort).

## Editor Integration

VS Code (formatOnSave via LSP):

1. Ensure the Aster LSP server is configured in your VS Code (via an extension or `settings.json`).
2. Add these settings to enable formatting on save and control the formatter mode:

```json
{
  "editor.formatOnSave": true,
  // LSP server settings
  "asterLanguageServer.format.mode": "lossless", // or "normalize"
  "asterLanguageServer.format.reflow": true
}
```

- Lossless preserves all existing trivia and applies only minimal seam fixes when `reflow` is true.
- Normalize enforces strict canonical output (same rules used by CI example formatting).

## CLI: Format Arbitrary Files

You can format any `.aster` file from the command line:

```bash
# Overwrite files in place (normalize)
npm run format:file -- --write path/to/file.aster

# Lossless print to stdout
npm run format:file -- --lossless path/to/file.aster

# Lossless with minimal seam reflow (to stdout)
npm run format:file -- --lossless --lossless-reflow path/to/file.aster

# Overwrite with lossless reflow
npm run format:file -- --write --lossless --lossless-reflow path/to/file.aster

# Normalize with inline comment preservation (best effort)
npm run format:file -- --write --preserve-comments path/to/file.aster
```


## Examples

Example CNL programs live in `test/cnl/examples`. JVM demo projects live under `examples/*` and assume generated classes are placed in `build/jvm-classes`:

```bash
# Arithmetic example end-to-end
./gradlew :aster-asm-emitter:run --args=build/jvm-classes < test/cnl/examples/arith_compare_core.json
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
