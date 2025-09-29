---
title: JVM Interop Overloads
---

# JVM Interop Overloads

This page explains how Aster chooses a Java overload for static calls and how you can disambiguate when needed.

## Policy (summary)

- Match by arity.
- Prefer exact primitive matches (I, J, D, Z), then primitive widening:
  - Int → Long, Double; Long → Double
- Prefer boxed types (Integer/Long/Double/Number) over `Object` when primitives don’t match.
- Core `Number` maps to JVM `java.lang.Double`. For Java callsites in examples, prefer `aster.runtime.Primitives.number(…)` to construct values.
- Prefer `String`/`CharSequence` for text.
- Varargs are considered with the component type.
- Ties are broken by a scoring heuristic; the emitter also attempts reflection against classes on the classpath to select the exact descriptor. If reflection is not available, a heuristic is used.

## Disambiguation

Use literal suffixes to make intent explicit:

- `1` is `Int` (I)
- `1L` is `Long` (J)
- `1.0` is `Double` (D)

For example, given `Interop.sum(int,int)`, `Interop.sum(long,long)`, and `Interop.sum(double,double)`:

- `Interop.sum(1, 2)` → picks `(II)`
- `Interop.sum(1L, 2L)` → picks `(JJ)`
- `Interop.sum(1.0, 2.0)` → picks `(DD)`
- Mixed:
  - `Interop.sum(1, 2L)` → widens to `(JJ)`
  - `Interop.sum(1, 2.0)` → widens to `(DD)`

The LSP highlights ambiguous calls that mix numeric kinds and offers a quick hint to use `1L` or `1.0`.

## Diagnostics

- When multiple overloads tie after scoring, the emitter logs an ambiguity note and the selected descriptor.
- When no primitive signals are present (e.g., only `Object`/`String` arguments), the emitter logs that a heuristic was used. Set `DIAG_OVERLOAD=false` to silence.

### Nullability Defaults/Overrides

For common helpers, parameters are treated as non‑null unless documented otherwise. Highlights:

- `Text.*`: all params are non‑null except `Text.equals(a,b)` which accepts nulls for both.
- `List.length(xs)`, `List.isEmpty(xs)`: `xs` non‑null. `List.get(xs,i)`: both non‑null.
- `Map.get(m,k)`: `m` non‑null, `k` may be null.
- `Map.containsKey(m,k)`: `m` non‑null, `k` may be null.
- `Set.contains/add/remove(s,x)`: `s` non‑null, `x` may be null.
- `Interop.sum(a,b)`: both non‑null.
- `Interop.pick(x)`: `x` may be null (Object overload will be chosen).

The LSP flags calls that pass `null` to parameters marked as non‑null.

You can enable strict enforcement during class emission by setting:

```
INTEROP_NULL_STRICT=true
```

In strict mode, emitting a class for a call that passes `null` to a non‑null parameter throws an error and fails the build. Otherwise, a warning is printed to stderr and emission proceeds.

You can also provide a custom policy JSON to override defaults:

```
INTEROP_NULL_POLICY=/absolute/path/to/interop-null-policy.json
```

Example file (see docs/examples/interop-null-policy.json):

```
{
  "Text.equals": [true, true],
  "Text.concat": [false, false],
  "Map.get": [false, true]
}
```

Each entry lists, per parameter (in order), whether `null` is allowed (`true`) or not (`false`).

Smoke test for strict mode:

```
npm run build
npm run verify:asm:nullstrict   # expects failure in strict mode for Text.length(null)
```

### Quick Fix Defaults (LSP)

When the LSP detects a null passed to a non‑null parameter, it offers a Quick Fix tailored to common methods:

| Method                  | Param | Suggested default |
|-------------------------|:-----:|-------------------|
| Text.split(h, sep)      |   2   | " "               |
| Text.startsWith(h, p)   | 1/2   | ""                |
| Text.endsWith(h, s)     | 1/2   | ""                |
| Text.indexOf(h, n)      |   2   | " "               |
| Text.contains(h, n)     |   2   | ""                |
|                         |   1   | ""                |
| Text.replace(h, t, r)   | any   | ""                |
| Text.toUpper(h)         |   1   | ""                |
| Text.toLower(h)         |   1   | ""                |
| Text.length(h)          |   1   | ""                |
| Text.concat(a, b)       | any   | ""                |
| List.get(xs, i)         |   2   | 0                 |
| Interop.sum(a, b)       | any   | 0                 |
| Map.get(m, k)           |   2   | ""                |
| Map.containsKey(m, k)   |   2   | ""                |
| Set.contains(s, x)      |   2   | ""                |
| Set.add(s, x)           |   2   | ""                |
| Set.remove(s, x)        |   2   | ""                |
| Interop.pick(x)         |   1   | ""                |

These defaults are conservative and intended to help quickly reach a valid state. You should review the replacement to ensure it matches your intended semantics.

### Lightweight Method Cache

The ASM emitter maintains a small cache of method descriptors under `build/.asteri/method-cache.json`. It is populated when reflection succeeds and used as a fallback when reflection is unavailable (e.g., restricted environments).

- Inspect cache: `npm run cache:inspect:methods`
- Clear cache: `npm run cache:clear:methods`

This improves stability and avoids repeated reflection during development. It is not a full classpath index.

## Examples

```cnl
This module is demo.examples.

To demoSum, produce Text:
  Return Interop.sum(1, 2L).  // widens to (JJ)

To demoSum2, produce Text:
  Return Interop.sum(1, 2.0). // widens to (DD)
```

```cnl
To pickKinds, produce Text:
  Return Interop.pick(1L). // (J)
```

### Java interop helpers

When constructing values from Java for fields or parameters typed as Aster `Number`, use the utility methods in `aster.runtime.Primitives` for clarity and consistency:

```
// timeOfDay is an Aster Number (emits as java.lang.Double)
var ctx = new demo.policy.PolicyContext(
  userId, userRole, owner, demo.policy.Resource.Document,
  aster.runtime.Primitives.number(14.5),
  "office");
```

Available helpers:

- `Primitives.number(double) -> Double`
- `Primitives.integer(int) -> Integer`
- `Primitives.longNum(long) -> Long`
- `Primitives.bool(boolean) -> Boolean`
