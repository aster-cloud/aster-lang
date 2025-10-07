# aster-lang — Language Reference v0.1

*Status: Draft (2025-09-30)*

This reference specifies the initial aster-lang core: syntax, types, effects, modules, and
durable workflow semantics. The design aims for human-friendly syntax with a strongly-typed,
deterministic core and first-class *capabilities* for effects (I/O, time, secrets, AI models, etc.).

## 1. Lexical Structure

- **Identifiers**: `[_A-Za-z][_A-Za-z0-9]*`
- **Keywords** (reserved): `module`, `import`, `type`, `record`, `enum`, `capabilities`, `uses`,
  `fn`, `workflow`, `on`, `start`, `step`, `retry`, `backoff`, `exp`, `let`, `set`, `if`, `else`,
  `match`, `Ok`, `Err`, `continue`, `stop`, `await`, `ensure`, `compensate`
- **Literals**: integers (`123`), decimals (`12.34`), strings (`"text"`), booleans (`true/false`),
  duration literals (`500ms`, `2s`, `3m`, `5h`, `2d`).
- **Comments**: line comments start with `//`; block comments `/* ... */`.

Whitespace and newlines are insignificant except inside strings.

## 2. Modules

Files begin with a `module` declaration and optional `import`s:
```aster
module shop.orders
import common.money
```

The module name maps to a package/namespace. Imports are qualified; wildcard imports are disallowed.

## 3. Types

### 3.1 Built-in types
- `Int`, `Long`, `Decimal(p,s)`, `String`, `Bool`, `Bytes`, `Time`, `Duration`, `UUID`

### 3.2 Type aliases
```aster
type Money = Decimal(precision=18, scale=2)
```

### 3.3 Records and Enums
```aster
record Item { sku: String, qty: Int, price: Money }
enum State { Created, Reserved, Pending, Cancelled, Completed }
```

### 3.4 Parametric types
```aster
type Result[T,E] = Ok(T) | Err(E)  // sugar for a sum type
```

### 3.5 Optionals
`T?` denotes an optional type. Pattern-match must handle the `None` branch explicitly.

## 4. Values, Variables, and Assignment

- `let name = expr` introduces an immutable binding.
- `set obj.field = expr` updates a mutable field in durable state *if the host structure is declared mutable*.
- Top-level `const` (future) is planned but omitted in v0.1.

## 5. Expressions and Statements

### 5.1 Expressions
Arithmetic, boolean ops, records `{}`, field access `.`, function calls `f(x,y)`.

### 5.2 Pattern matching
```aster
match r {
  Ok(v)  => doSomething(v)
  Err(e) => handle(e)
}
```

### 5.3 Conditionals
```aster
if cond { ... } else { ... }
```

### 5.4 Blocks
Blocks are delimited by braces `{ ... }`. A block’s final expression is the value (if any).

## 6. Functions

```aster
fn charge(o: Order, amount: Money) -> Result[Receipt, PaymentError] uses [Http, Sql] {
  // body
}
```
- **Effect declaration** after `uses` lists required capabilities. The effect checker enforces calls.
- Functions are pure unless they declare effects. Pure functions can call only pure functions.

## 7. Capabilities & Effects

Effects are declared as *capabilities* and imported at module or function scope:
```aster
capabilities uses [Sql, Http["https://api.payments"], Email, Time, Secrets["kv/data"]]
```
Capabilities can be parameterized. A function/workflow may further restrict effects via its own `uses`.

**Standard capability kinds (v0.1):**
- `Time` — durable timers, current time query (non-deterministic time must be recorded).
- `Sql[dsn: DSN, tx: TxMode]` — transactional DB access; outbox/inbox for exactly-once.
- `Http[domain: Host]` — HTTP client with idempotency keys & retry policy integration.
- `Email` — SMTP/transport abstraction.
- `Secrets[path: VaultPath]` — secret retrieval; never printable.
- `AiModel[model: String, budget: Decimal]` — constrained AI calls with prompt hygiene.

## 8. Workflows (Durable Orchestrations)

Workflows are long-running, event-sourced processes with deterministic replays.
```aster
workflow Fulfill(o: Order) uses [Sql, Http, Email, Time] {
  on start:
    ensure o.total >= 0

  step reserve = retry(max=5, backoff=exp(500ms)) {
    match reserveStock(o) {
      Ok(_)  => continue
      Err(e) => { notifySupport(e, o); set o.state = Pending; stop }
    }
  }

  step pay = {
    let r = charge(o, o.total)
    match r {
      Ok(_)  => set o.state = Completed
      Err(_) => { compensate reserve; set o.state = Cancelled }
    }
  }

  await Time.after(24h)
  if o.state == Pending { remind(o.email) }
}
```
### 8.1 Steps
Each `step` is a transactional unit with an optional retry policy. On replay, recorded decisions are honored.

### 8.2 Retry policies
`retry(max=N, backoff=exp(X)|fixed(X), jitter=?true)`

### 8.3 Compensation
`compensate stepName` invokes the registered compensation handler for that step (if any).

### 8.4 Timers
`await Time.after(3d)` creates a durable timer. Timers are coalesced by the runtime where possible.

### 8.5 Signals & Queries (v0.2+)
Planned additions for external event injection (`signal`) and read-only inspection (`query`).

## 9. Determinism Contract

Workflow logic must be deterministic during replay. Non-deterministic sources (time, random, network) are modeled as *commands* with recorded results. Calls outside capabilities are rejected by the checker.

## 10. Interop

- JVM interop via generated wrappers. External libraries are imported as modules with declared effects.
- Data codecs derive automatically for `record`/`enum` to JSON/Avro/Protobuf.

## 11. Testing

- Property-based tests with generators for ADTs.
- Effectful code can be tested against *simulators* that implement capability interfaces.

## 12. Tooling

- `asterpm` will support: `build`, `fmt`, `check`, `doc`, `run`.
- LSP: completion, hover with effect signatures, quick-fixes for missing capabilities.

## 13. Grammar Notes (ANTLR)

See `grammar/AsterLexer.g4` and `grammar/AsterParser.g4`. Grammar is intentionally minimal in v0.1 and will evolve with the spec.

## 14. Versioning & Stability

- Language is versioned; breaking changes require a migration guide.
- Capabilities are versioned independently; stable interfaces preferred.

---

**Non-goals (v0.1):** traits/typeclasses, macros, metaprogramming. These may be explored after core stability.
