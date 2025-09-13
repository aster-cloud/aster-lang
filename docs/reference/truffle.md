# Truffle Interpreter (Spike)

This module (`truffle/`) hosts an experimental Truffle-based interpreter for a
small subset of Aster's Core IR.

## Status

- Minimal nodes: `LiteralNode`, `ReturnNode`, `IfNode`, `LetNode`, `AsterRootNode`.
- Demo runner: `aster.truffle.Runner` shows a tiny if/return program and prints a result.
- Next: Build nodes from Core IR JSON and execute simple functions end-to-end.

## Running the Demo

```
./gradlew :truffle:run
```

## Roadmap

- Add a JSON loader: parse Core IR JSON (same shape as ASM emitter) and construct Truffle nodes.
- Implement basic expressions: Name lookup, Call (host calls for simple math/text), Return, If.
- Add simple environment and frame slots for let-bindings; add counters for profiling.
- Integrate with the main toolchain: `aster core <file>` → JSON → Truffle run.

## Nested Pattern Smoke (Core JSON)

Run the nested pattern lambda fixtures through Truffle:

```
# Outer(Inner(x)) → prints 42
npm run truffle:run:nested

# Outer(Mid(Inner(x))) → prints 7
npm run truffle:run:nested:deep
```
