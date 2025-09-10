# Repository Guidelines

## Project Structure & Module Organization
- `src/` — TypeScript compiler pipeline (canonicalizer, lexer, parser, Core IR, JVM emitter, LSP).
- `scripts/` — build/test utilities run via `ts-node` (PEG build, golden, emit/jar, REPL).
- `test/` — property, fuzz, and benchmark tests.
- `cnl/examples/` — sample programs and golden fixtures.
- `docs/` — VitePress site; `dist/` — build output.
- Gradle modules: `aster-asm-emitter/`, `truffle/`, `examples/*` (Java 17 toolchain).

## Build, Test, and Development Commands
- Install: `npm ci` (or `npm install`). Build: `npm run build` (tsc + PEG generation).
- Watch: `npm run dev`. Type check: `npm run typecheck`.
- Lint/format: `npm run lint`, `npm run lint:fix`, `npm run format`.
- Tests: `npm test` (golden + property), fuzz: `npm run test:fuzz`, bench: `npm run bench`.
- CLI example: `npm run build && node dist/scripts/cli.js cnl/examples/greet.cnl`.
- JVM demos: e.g., `npm run emit:class` then `./gradlew :examples:login-jvm:run`.

## Coding Style & Naming Conventions
- TypeScript: strict mode; explicit function return types; avoid `any`; prefer `const`; `eqeqeq`.
- Formatting (Prettier): 2 spaces, single quotes, trailing commas, 100-char width.
- ESM only (no CommonJS). Keep modules small and pure; favor `readonly` data.
- Naming: files kebab-case (`lexer.ts`), functions camelCase (`parseExpr`), types/classes PascalCase (`TokenKind`), constants UPPER_SNAKE_CASE.

## Testing Guidelines
- Property tests with fast-check: `npm run test:property`.
- Golden tests compare outputs in `cnl/examples/`; update with `npm run test:golden:update` and review diffs.
- Fuzz and benchmarks live in `test/`; keep additions deterministic or seedable.
- Prefer small, focused tests near existing ones; cover new IR/AST nodes end-to-end.

## Commit & Pull Request Guidelines
- Use Conventional Commits (e.g., `feat:`, `fix:`, `docs:`). Add a changeset when user-facing: `npm run changeset`.
- Before opening a PR: `npm run ci` (typecheck, lint, tests, build, smoke checks).
- PRs must include: clear description, linked issues, test updates, and example inputs/outputs when relevant.
- Do not commit `dist/` or generated artifacts. Keep changes minimal; avoid drive-by reformatting.

## Environment & Tips
- Requirements: Node 18+, npm; Java 17 for Gradle modules. macOS/Linux recommended.
- For Graal/Truffle work, ensure a JDK 17 toolchain and set `JAVA_HOME` appropriately.
