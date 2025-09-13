# JVM Interop (MVP)

This note outlines a minimal, pragmatic interop surface between Aster and the JVM.
The near-term goal is to make common string and collection operations map to idiomatic
Java while keeping Aster’s CNL readable and portable.

## Naming and Calls

- Aster calls are plain names, e.g., `Text.concat(a, b)` or `List.length(xs)`.
- The JVM emitter recognizes certain names and lowers them to Java idioms:
  - `Text.concat(a, b)` → `a + b`
  - `Text.contains(h, n)` → `h.contains(n)`
  - `Text.equals(a, b)` → `Objects.equals(a, b)`
  - `Text.replace(h, t, r)` → `h.replace(t, r)`
  - `Text.split(h, sep)` → `Arrays.asList(h.split(sep))`
  - `Text.indexOf(h, n)` → `h.indexOf(n)`
  - `Text.startsWith(h, p)` → `h.startsWith(p)`
  - `Text.endsWith(h, s)` → `h.endsWith(s)`
  - `Text.toUpper(h)` → `h.toUpperCase()`; `Text.toLower(h)` → `h.toLowerCase()`
  - `Text.length(h)` → `h.length()`
  - `List.length(xs)` → `xs.size()`; `List.get(xs, i)` → `xs.get(i)`; `List.isEmpty(xs)` → `xs.isEmpty()`; `List.head(xs)` → `xs.isEmpty()? null : xs.get(0)`
  - `Map.get(m, k)` → `m.get(k)`
- Unrecognized calls are emitted as regular Java calls on the rendered name; use
  imports/module names to fully qualify generated functions (e.g., `app.fn.id(...)`).

## Types

- `Text` → `String`
- `Int` → `int`, `Bool` → `boolean`
- `Maybe<T>` → `T` (nullable)
- `Result<Ok, Err>` → `aster.runtime.Result<Ok, Err>` (bridge types live in `aster-runtime`)
- `List<T>` → `java.util.List<T>`; `Map<K,V>` → `java.util.Map<K,V>`

## Exceptions (Preview)

- Prioritize `Result<Ok, Err>` for flow-control over exceptions in CNL.
- Mapping Java exceptions to `Result.Err` will be added in a future pass, with
  conveniences to catch and wrap.

## Collections Bridge (Preview)

- Future work: `List.map`, `filter`, `fold` via Java streams or loops. For now,
  examples compile to Core IR; the JVM emitter only recognizes the small set above.

## Configuration

- Interop mappings live in the emitter (`src/jvm/emitter.ts`). As the stdlib evolves,
  add safe, common shims that preserve semantics.

## Roadmap

- Add safe wrappers for `Result` from exceptions (try/catch to `Err`).
- Extend `Text` and `List`/`Map` coverage.
- Document module-to-package interop patterns for calling into existing Java libs.
