# Parametric Polymorphism (Preview)

This document proposes a minimal, readable generics surface for Aster and the
corresponding Core IR representation. The goal is to support generic types in
user-defined data and function signatures with clear, CNL-style syntax.

## Type Application

- Syntax: `Foo of T` or `Pair of A and B`.
- Multiple type arguments use `and` or commas.
- Applies to any type constructor name (user data or library types).

Examples:

```
Define a Box of T with value: T.
Define a Pair of A and B with left: A and right: B.

To left, with p: Pair of A and B, produce A:
  Return p.left.
```

IR shape:

- AST: `TypeApp { base: 'Pair', args: [Type, ...] }`
- Core: `TypeApp { base: 'Pair', args: [Type, ...] }`

## Type Variables

- Written as capital identifiers (e.g., `T`, `A`, `Key`).
- In this preview, type variables are placeholders; inference and checking are limited.
- Core includes `TypeVar` for future use; current typechecker treats unknowns permissively.

## Functions (Future Phase)

- Function type parameters (implemented):

```
To identity of T, with x: T, produce T:
  Return x.

To first of A and B, with p: Pair of A and B, produce A:
  Return p.left.
```

- Parsing attaches `typeParams: ["T", ...]` to the function.
- Lowering replaces any type name matching a type parameter with a Core `TypeVar`.
- Typecheck diagnostics:
  - Error if a type variable-like name is used but not declared:
    - “Type variable-like 'U' is used in 'foo' but not declared; declare it with 'of U'.”
  - Warning if a declared type parameter is unused.

Inference (preview):
- The checker attempts a minimal unification from the return position to bind type variables (warnings on inconsistent inference).

## Typechecking (Initial Behavior)

- `TypeApp` participates in formatting and lowering; strict unification is future work.
- `TypeVar` prints as its name and is treated permissively in equality.

## JVM Emission

- Unknown generics map to `Object` for now. Built-ins like `List` and `Map`
  already have dedicated mappings.

## Roadmap

- Add `typeParams` on functions and data declarations.
- Add unification-based inference for `TypeVar` and `TypeApp`.
- Enrich emitter with generic-aware bridges where applicable.
