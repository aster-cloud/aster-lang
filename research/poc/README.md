# aster-lang v0.1 — Starter Pack

This starter pack contains:
- **LANGUAGE_REFERENCE.md** — the v0.1 language reference (spec).
- **grammar/** — ANTLR v4 grammar (`AsterLexer.g4`, `AsterParser.g4`).
- **compiler/** — Java compiler skeleton with Gradle build.
- **quarkus-worker/** — Quarkus worker scaffold (Gradle).
- **examples/** — Three illustrative aster-lang samples:

  - `order_fulfillment.aster`: Durable saga with retries and compensations.
  - `data_pipeline.aster`: Idempotent ETL steps with timers.
  - `ai_human_in_loop.aster`: Human-in-the-loop AI step with policy guard.

## Quick start

```bash
# (1) Explore the spec
open LANGUAGE_REFERENCE.md

# (2) Grammar
cd grammar
# Import the .g4 files into your ANTLR4-enabled IDE or use antlr4 toolchain

# (3) Compiler skeleton
cd compiler
./gradlew build  # (or 'gradle build' if Gradle wrapper is present)

# (4) Quarkus worker (skeleton service)
cd ../quarkus-worker
./gradlew quarkusDev
# Visit: http://localhost:8080/hello
```

> Note: These are *skeletons* to accelerate development. They are not feature-complete.
