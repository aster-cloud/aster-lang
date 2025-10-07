# Capability-Parameterized Effects (Design)

Status: Design accepted; parsing enabled for CNL-first and bracket sugar; enforcement behind flag
Owner: Compiler team

## Goals

- Express intent about which I/O capabilities a function may use (Http, Sql, Time, Files, Secrets, AiModel, …).
- Preserve the minimal lattice: ∅ ⊑ CPU ⊑ IO[*]. IO subsumes CPU.
- Ship CNL-first syntax now; keep IR/backends backward-compatible; gate enforcement behind a feature flag.

Non-goals (Phase 0):
- Parametric capability arguments (e.g., Http[method=POST]).
- Cross-module capability inference.
- Runtime enforcement.

## Surface syntax (CNL-first)

- Header clause remains the source of truth. Two notations (equivalent):
  - Fully CNL: "It performs io with Http and Sql and Time."
  - Bracket sugar: "It performs io [Http, Sql, Time]."

Notes:
- Capitalization of capability names is stylistic; parser accepts `Http`, `SQL`, `Time` as identifiers.
- Capability list is optional; plain `It performs io.` is valid and means unconstrained IO.
- Formatter: example files are normalized to bracket sugar when capability lists are present.

### Choosing a style

- Prefer bracket sugar in headers when listing more than one capability: concise and unambiguous.
- Use CNL form in prose-heavy examples or when teaching the language step-by-step.
- Both forms are equivalent; the parser accepts either. The formatter normalizes example headers with lists to bracket sugar.


Notes:
- Capitalization of capability names is stylistic; parser should accept `Http`, `SQL`, `Time` as identifiers.
- Capability list is optional; plain `It performs IO.` still valid and means unconstrained IO.

## AST and IR representation

Current IR: `Func.effects: readonly Effect[]` with `Effect = IO | CPU`.

Additive metadata for backward compatibility:
- AST.Function: add optional `effectCaps?: { io?: string[] }`.
- Core.Func: add optional `effectCaps?: { readonly io?: readonly string[] }`.

Rationale:
- Keeps existing `effects` array unchanged (minimizes churn in emitters/tests).
- Allows tools to opt-in to capability-aware behavior without breaking older code.

### Example (conceptual)

### Examples

- With capability list (CNL):

  To ping, produce Text. It performs io with Http and Sql and Time:
    Return "ok".

- With capability list (brackets):

  To ping, produce Text. It performs io [Http, Sql, Time]:
    Return "ok".

- Unconstrained IO (no list):


### Capability registry

The prefixes that map call sites to capabilities live in:
- src/config/effects.ts → CAPABILITY_PREFIXES
  - Example mappings: Http → ['Http.'], Sql → ['Db.', 'Sql.'], Time → ['Time.', 'Clock.'], Files → ['Files.', 'Fs.'], Secrets → ['Secrets.'], AiModel → ['Ai.']

This table is used under ASTER_CAP_EFFECTS_ENFORCE=1 to check that used capabilities are a subset of those declared in the function header.

### Mixed-form header example

The parser also accepts a mixed form combining `with` and bracket list:

  To ping, produce Text. It performs io with [Http, Sql]:
    Return "ok".

  To ping, produce Text. It performs io:
    Return "ok".

- CNL: "It performs io with Http and Sql and Time."
- AST/Core: `effects = [IO]`, `effectCaps.io = ["Http", "Sql", "Time"]`.

## Parser changes

- Extend effect list parsing to accept optional CNL clause after `io`:
  - Grammar sketch (CNL form):
    - `It performs io (with Capability (and Capability)*)? .`
  - Bracket sugar is deferred until lexer exposes '[' ']'.
- The parser records capability identifiers but does not validate their existence or semantics.

Status:
- Implemented: CNL form is accepted and captured; bracket sugar is tokenized and parsed (lexer supports '[' and ']').

## Lowering

- Pass through `effectCaps` from AST to Core.Func unchanged.
- No changes to current Effect enum or downstream emitters.

## Typechecker (feature-gated, planned)

- Default behavior (today):
  - Enforce minimal lattice only (missing effects → errors; IO ⊒ CPU).
  - If `effectCaps` present, do not enforce capabilities yet; no diagnostics solely for caps presence.

- Gated behavior (future; `ASTER_CAP_EFFECTS_ENFORCE=1`):
  - Define a mapping from callsite prefixes to capability tags in `src/config/effects.ts`:
    - e.g., `Http`: ["Http.", "Fetch."]
    - `Sql`: ["Db.", "Jdbc."]
    - `Time`: ["Time.", "Clock."]
  - Collect used capability tags in function body from the mapping.
  - Check that `usedCaps ⊆ declaredCaps` when `@io` has a list; else if `@io` without list → unconstrained, allow all.
  - Diagnostics:
    - Missing capability: error (code `EFF_CAP_MISSING`)
    - Superfluous capability in header: info (code `EFF_CAP_SUPERFLUOUS`)

### Subsumption and join (future)
- Within IO, treat capability sets with subset ordering:
  - `IO[A] ⊑ IO[B]` iff `A ⊆ B`.
  - Join: `IO[A] ⊔ IO[B] = IO[A ∪ B]`.
- Functions without a list are considered `IO[*]` (top). CPU remains below IO.

## Feature flags

- `ASTER_CAP_EFFECTS=1`
  - Enable parsing and IR emission of `effectCaps` (safe; default can be ON).
- `ASTER_CAP_EFFECTS_ENFORCE=1`
  - Enable capability subset checks in the typechecker (default OFF until stable).

## Migration & compatibility

- Backward-compatible: existing programs with `It performs IO/CPU` continue to work.
- Capability lists are opt-in metadata until enforcement is enabled.
- Tooling (LSP) can surface capability info in hovers and code actions without enforcing.

## Acceptance criteria (for this design slice)

- Parser accepts CNL capability lists and stores them in AST; bracket sugar tracked as TODO.
- Lowering preserves `effectCaps` to Core.Func.
- No enforcement by default; gated enforcement design is documented.
- Add or update docs (this page; references in DESIGN.md) and at least one golden showing capability list in AST/CORE when we enable printing of metadata (optional).

## Open questions

- Canonical names and casing for capability tags (Http vs HTTP).
- Extensibility for parameterized capabilities (e.g., Http[method=POST]).
- Interactions with effect polymorphism (`with E`) when E contains IO rows.

