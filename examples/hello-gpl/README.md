# Aster Project Scaffold

This folder was created by Aster's scaffold tool. It contains a minimal CNL program at `cnl/main.aster`.

## Files

- `cnl/main.aster` — demo module with `hello` and `greet`.

## Try It (from aster-lang repo root)

1) Build the compiler once:

   npm run build

2) Parse to AST:

   node dist/scripts/cli.js examples/hello-gpl/cnl/main.aster

3) Emit Core IR:

   node dist/scripts/emit-core.js examples/hello-gpl/cnl/main.aster

4) Emit JVM class files and create a jar:

   node dist/scripts/emit-classfiles.js examples/hello-gpl/cnl/main.aster
   node --loader ts-node/esm scripts/jar-jvm.ts

The generated jar is written to `build/aster-out/aster.jar`. You can attach it to the example Gradle apps under `examples/*` (they already depend on that jar).

