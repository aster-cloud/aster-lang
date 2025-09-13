# Effects & Async (MVP)

Aster distinguishes between pure computation and effectful work using simple,
readable annotations and a small set of control constructs for concurrency.

## Declaring Effects

- Syntax (function header): `It performs IO`, `It performs CPU`, or both.
- Examples:

```
To fetch user with id: Text, produce Result of User or AuthErr. It performs IO:
  Return AuthRepo.load(id).

To checksum with bytes: List of Int, produce Int. It performs CPU:
  Return Crypto.hash(bytes).
```

- Effects are advisory in the MVP. The typechecker scans calls using a small
  registry of prefixes (`src/config/effects.ts`) and emits warnings if a
  function likely performs I/O or CPU work without declaring it (or vice versa).

## Async Concurrency

Two constructs make async execution explicit and readable:

- `Start x as async Expr.` begins an async task and binds a handle to `x`.
- `Wait for x and y.` waits for previously started tasks to complete.

Example:

```
Within a scope:
  Start profile as async ProfileSvc.load(u.id).
  Start timeline as async FeedSvc.timeline(u.id).
  Wait for profile and timeline.
  Return Ok of Dash(profile, timeline).
```

Guidance and lints:

- The typechecker warns if you `Start` tasks that are never `Wait`ed.
- `await(expr)` is an expression form; it expects `Maybe<T>` or `Result<T,E>` and
  yields `T`, otherwise a warning is emitted.
- Async itself doesn’t imply IO; however, in practice async often wraps IO calls.
  The effects scan includes expressions under `Start`.

## Semantics (Preview)

- Effects are not enforced by the runtime; they are compile-time contracts and lints.
- Async is structured: tasks are scoped to blocks; `Start` must be paired with a
  `Wait` to avoid leaks (the linter warns when not matched).
- Evaluation order within a `Within a scope:` block is left-to-right for `Start`
  statements, with `Wait` establishing a join point.

## Configuration

- Tune detection by editing `src/config/effects.ts` to add prefixes that count as
  `IO` or `CPU` for your codebase.

## Future Work

- Effect rows, propagation, and precise analysis.
- Better async typing for task handles and structured `awaitAll` semantics.
- Diagnostics for blocking operations in `@cpu` contexts.

