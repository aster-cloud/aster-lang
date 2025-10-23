# ASM Emitter Coverage & Validation (MVP)

The `aster-asm-emitter` Gradle module turns Core IR JSON into JVM class files
using ASM. Coverage is intentionally small but growing.

## What It Handles Today

- Data classes with public final fields and a generated constructor.
- Enums (constants only, no bodies).
- Functions (static methods) with a subset of statements:
  - `Let`, `If` (with optional else), `Match` (on `null`, data ctor, enum variants with tableswitch), `Return`.
  - Simple call patterns for numeric ops and boolean `not`.
  - Ok/Err construction (erased to `aster.runtime.Result` hierarchy).

What it does not handle yet:
- Async (`Start`, `Wait`), `Scope` lowering is partial.
- Loops, and full interop/generics.
- Lambda/function values (see Lambdas & Closures draft).

## Quick Validation

Run class emission on representative programs and disassemble with `javap`:

```
npm run verify:asm
```

This compiles `test/cnl/examples/login.aster` and `test/cnl/examples/greet.aster` to
`build/jvm-classes`, then runs `javap -v` across all `.class` files.

## Recent Coverage Additions

- Enum constants as expressions (`InvalidCreds`) emit `getstatic` on the enum type.
- Enum matches: optimized `tableswitch` on `.ordinal()` when exhaustive over a single enum.
- Fallback enum/name matches: reference equality checks; wildcard `PatName` acts as catch-all.
- Try/catch wrapper for `Result` returns: interop calls producing `Ok/Err` via exception-to-string mapping.

## Roadmap

- Support nested destructuring in `Match`.
- Lambdas & closures: see [Lambdas & Closures (Design Draft)](./lambdas.md).
- Broaden expression coverage and basic loops.
