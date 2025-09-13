# Modules & Imports (MVP)

This document specifies the minimal module system for Aster’s experimental preview.
It focuses on readable declarations, simple imports, and JVM‑friendly namespacing.

## Module Declarations

- Syntax: `This module is app.service.` (case‑insensitive)
- `app.service` is a dotted module name. If omitted, the module name is `null`.
- Backends map the module name to target packages, e.g., JVM packages and directories.

Examples:

```
This module is app.
This module is app.service.
```

## Imports

- Syntax: `Use pkg.name.` or `Use pkg.name as alias.`
- Multiple imports are allowed. Aliases must be unique within a module.
- Semantics (MVP): imports establish readable namespacing for code and tooling. Full name resolution is pending future phases; the typechecker does not yet resolve imported symbols.

Examples:

```
Use app.service.
Use third.party.uuid as UUID.
```

## Visibility (Preview)

- All declarations are visible within a module (no explicit `export`/`private` yet).
- Cross‑module visibility is implicit: when compiled to JVM, all generated classes/functions are public; future phases will define finer‑grained controls.

## Namespacing & Qualification

- Use dotted names to qualify calls and types in CNL (e.g., `AuthRepo.verify`).
- The JVM emitter places generated classes under `build/jvm-src/<module path>/` and sets the Java `package` to the module name, so `app.service.login_fn` becomes `package app.service;` with class `login_fn`.

## Package Layout

- Source files can live anywhere; module identity comes from the `This module is …` header.
- Emitted Java source/bytecode are laid out by module name:
  - Java sources: `build/jvm-src/<dotted-module-as-path>/*.java`
  - Class files: `build/jvm-classes/<dotted-module-as-path>/*.class`

## Future Work

- Name resolution across imports, shadowing, and cycles.
- Explicit visibility (`export`, `internal`, `private`) and re‑exports.
- Module paths → project/workspace packaging and dependency management.
- Exhaustiveness and diagnostics that reference imported types/functions.

