# Lambdas & Closures (Design Draft)
Status: Draft | Last updated: 2025-10-07 12:06 NZST | Maintainer: Codex

This document outlines a minimal design to add first-class function values to
the Core IR and JVM emitter. The goals are predictable lowering, simple
runtime interop, and an incremental path that doesn’t block existing features.

## Goals

- First-class function values: pass as arguments, return from functions, store in
  data fields.
- Support simple closures that capture immutable locals from the defining scope.
- Keep the surface syntax natural (CNL), but start with IR support first.
- JVM emission that works without invokedynamic; use plain classes + interfaces.

## IR Additions

- Type: `FuncType { params: Type[], ret: Type }`
  - Printable as `(A, B) -> C` in pretty printer.
- Expr: `Lambda { params: Param[], ret: Type, body: Block, captures?: string[] }`
  - `captures` is populated during lowering via capture analysis.
- Expr: `Call` already exists; allow `target` to be any `Expr` of `FuncType`.

Notes:
- This does not introduce higher-kinded types or generics on lambdas; those can come later.

## Lowering

- Surface syntax (future):
  - Inline lambda: `Return (x) -> x + 1.`
  - Named function as value: `Return id.` (already supported as a Name literal)
- For now: enable IR-only by allowing tests/fixtures to inject `Lambda` nodes.
- Capture analysis:
  - Determine free variables of the lambda body not bound by its params.
  - Validate they are immutable locals from the enclosing scope.
  - Populate `captures` in a stable declaration order.

## Runtime Interfaces

- Add simple functional interfaces in `aster-runtime`:
  - `Fn1<T,R> { R apply(T a); }`
  - `Fn2<A,B,R> { R apply(A a, B b); }`
  - Extend as needed; start with `Fn1`.
- Map `FuncType` arity `N` to `FnN` in JVM emission.

## JVM Emission

- For each `Lambda` expression:
  - Synthesize a final class (e.g., `${enclosing}_lambda$<seq>`) with:
    - Final fields for each captured variable.
    - A constructor taking captured values and storing them.
    - An `apply` method matching `FnN` to evaluate the lambda body.
  - Emit a `new` of that class at the lambda site, passing captures.
  - The value’s static type is the corresponding `FnN` interface.
- Calls:
  - If target has `FuncType` of arity `N`, emit `invokeinterface FnN.apply`.
- Interop:
  - In the future, provide adapters to Java `java.util.function.*` where helpful.

## Validation Plan

- Golden/ASM fixtures:
  - Pass a simple `(x) -> x` into `List.map` and verify bytecode.
  - Closure capturing an outer `prefix` string used in the body.
- Truffle:
  - Add `LambdaNode` and `CallFunctionNode` to execute closures.

## Out of Scope (Initial)

- Partial application, currying, higher-kinded generics, and invokedynamic.
- Precise escape analysis for captured variables (always heap fields initially).

## Open Questions

- Surface syntax for multiple parameters in CNL without punctuation.
- Whether to allow capturing mutable vars (likely no; require immutability).
- Proposed CNL Syntax (Not Implemented)

To keep the CNL surface natural, one option is to model inline lambdas with a
compact phrase that mirrors function headers, for example:

```
Return a function with x: Text, produce Text:
  Return Text.concat("Hi, ", x).
```

Or a short-form inline expression:

```
Return (x: Text) => Text.concat("Hi, ", x).
```

Notes:
- The first form reuses existing header words (with/produce) and block shape.
- The short-form introduces a minimal `=>` token for expression lambdas.
- Both forms lower to `Core.Lambda` with `captures` derived from free
  variables. Block form is implemented; short-form parsing is supported for
  simple cases and infers common return types.
## Usage

Two forms are supported in CNL:

- Block form (with a body block):

```
Let f be function with x: Text, produce Text:
  Return Text.concat(pfx, x).
Return f("there").
```

- Short form (inline expression):

```
Let g be (y: Text) => Text.concat("2", y).
Return g("b").
```

Notes:
- Block-form lambdas end the header with a colon, followed by an indented block.
- Short-form return types are inferred for common cases: Text.concat → Text, (+) → Int, not/</>/== → Bool; otherwise Unknown.
- Both forms lower to Core.Lambda with captures derived from free variables.

## Verification

From Core JSON (extended lambda fixtures):

```
npm run verify:asm:lambda
```

From CNL examples end-to-end (parse → lower → emit ASM → javap):

```
npm run verify:asm:lambda:cnl
```

This emits classes for the lambda examples in `test/cnl/examples/` and runs `javap -v` on all emitted `.class` files.
