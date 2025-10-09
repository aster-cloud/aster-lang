Status: Active | Last updated: 2025-10-07 12:06 NZST | Maintainer: Codex

A PhD-level technical analysis of aster-lang (human-friendly, workflow-first, AI-native)

Below is a pragmatic, end-to-end view of what aster-lang can (and should) be to stand out—grounded in PL theory, modern compiler architecture, and real-world platform concerns.

⸻

1) Positioning & thesis

Thesis: aster-lang is a workflow-first, human-friendly programming language that compiles to robust, auditable, cloud runtimes (initially the JVM via GraalVM), while offering AI-assisted authoring that is constrained by a formal core language.
This avoids the trap of “NL → code” magic: natural language (NL) is a companion interface, not the semantic ground truth. The ground truth is a compact, strongly-typed core language that is easy to read, verify, test, and deploy.

What makes it different vs. Kotlin/Scala/DSLs/Temporal YAML/etc.:
•	Unified model: durable workflows, services, and data shapes expressed in one language with one type system and one build toolchain (no glue YAML).
•	Effects as first-class: retries, backoff, timeouts, compensations, idempotency, message semantics (at-least/exactly-once) are type-checked capabilities, not comments in README.
•	AI-native editor: NL prompts produce type-checked code stubs under tight constraints (Controlled NL → Core aster), with explorable diffs and provenance.
•	Determinism tiers: deterministic core for reliability; opt-in “agentic” steps sandboxed by capability types and policy guards.

⸻

2) Language core (what you must pin down early)

Paradigm: strict, statically typed, expression-oriented; lightweight syntax (indentation or braces—pick one and stick to it).
Types: algebraic data types (ADTs), records, enums, generics, optional/nullable (explicit), string templates with sanitization, decimal (money-safe), time types.
Type inference: Hindley–Milner-style local inference with principal types; overload resolution kept minimal to preserve predictability.

Effects & capabilities (the killer feature):
Adopt a lightweight capability effect system:

capabilities:
Http[domain: Host, method: GET|POST, retry: Policy]
Sql[db: DSN, tx: TxMode]
Time
Files
Secrets[scope: VaultPath]
AiModel[model: "gpt-4.1", budget: Dollars]

	•	A function declares what it can do; the compiler enforces effects.
	•	Workflows carry long-running/durable variants of effects (DurableTime.sleep, DurableSql, etc.).

Errors: typed results (Result<T,E>), pattern-matching; no hidden exceptions in user space. Interop layers can translate checked/unchecked to Result.

Modules & imports: deterministic, explicit versions; no magic global state.

⸻

3) Workflow semantics (durability and correctness)

Execution model: event-sourced durable orchestrations (akin to Temporal/Cadence), but surfaced as language constructs:

workflow OnboardUser(id: UserId) uses [Sql, Email, Time] {
let user = createUser(id)                            // persisted
sendWelcomeEmail(user.email)                         // side-effect w/ retry
await Time.after(3 days)                             // durable timer
if not user.confirmed:
cancelAccount(id) compensate createUser           // saga compensation
}

Built-ins you should formalize:
•	Retry policy syntax (with jitter, exponential backoff).
•	Idempotency keys baked into calls.
•	Compensations as duals of steps (Sagas).
•	Signals (external events) and queries (side-effect-free inspection).
•	Determinism contract for workflow logic (no nondeterministic sources unless wrapped as commands recorded in the event log).

⸻

4) Natural language (NL) as a constrained front-end, not the language

Controlled Natural Language (CNL): Define a small, unambiguous CNL that maps 1:1 to aster constructs. Example:

When an order is created, reserve stock. If reservation fails, notify support and mark order pending.
Retry reservation up to 5 times with exponential backoff starting at 500 ms.

→ Compiler’s NL front-end emits:
•	A typed workflow OrderCreated + reserveStock() step
•	A retry block with the concrete policy
•	An explicit error branch producing notifySupport() and state update

LLM role: propose code within the CNL template or directly produce core aster with inline rationales as comments. All suggestions must type-check before acceptance. Keep a provenance ledger in comments/metadata (“Generated from prompt X on 2025-09-30”).

⸻

5) Architecture (compiler & runtime)

Frontend:
•	Lexer/Parser: ANTLR or a Pratt parser (your choice).
•	AST → typed AST (name resolution, kind checking for generics).
•	Effect checking: capability constraints; flow graph analysis for compensations.

IR (Aster-IR):
•	SSA-like with effect annotations (e.g., call Http[...]), checkpoint points for durability, and compensation edges.

Backends (prioritise JVM first):
1.	JVM bytecode via ASM or via a high-quality Java/Kotlin transpiler first (faster bootstrap) + JIT via GraalVM.
2.	TS/Node backend for lightweight workers (Phase 2).
3.	Python backend for data workflows (Phase 3).

Runtime:
•	Durable orchestration engine (start with a wrapper over Temporal or your own lightweight event-sourced engine).
•	Activity workers (Quarkus).
•	Outbox/Inbox tables for exactly-once (via PostgreSQL).
•	Secret management via Vault capabilities.
•	Observability: OpenTelemetry traces w/ activity and step IDs.

⸻

6) Concurrency model
   •	Async/await + structured concurrency (scopes cancel children on failure).
   •	Channels for bounded fan-out/fan-in; supervision trees for workflow steps.
   •	Deterministic replay rules (no ambient randomness/time).

⸻

7) Interop story (win or lose factor)
   •	JVM interop must be ergonomic: import jars as modules, auto-generate aster capability wrappers with declared effects (e.g., Http, Jdbc, S3).
   •	Data mapping: records ↔ POJOs ↔ JSON/Avro/Protobuf via derived codecs.
   •	Vaadin/Quarkus: generate service stubs, REST endpoints, and simple Vaadin route scaffolds from aster specs.

⸻

8) Security & policy
   •	Capabilities gate access. An “agentic” AI step must declare AiModel[budget: $X] and a policy guard:
   •	allow only GETs to whitelisted domains,
   •	redact secrets in prompts,
   •	require human-in-the-loop for high-risk actions (compile-time and runtime policy).
   •	Auditing: every side effect gets a correlation ID and appears in structured logs.

⸻

9) Formal assurance (lightweight but meaningful)
   •	Progress & safety invariants as executable specs (refined types or contracts):

invariant Order.total >= 0
invariant State in {Created, Reserved, Pending, Cancelled, Completed}

	•	Property-based testing harness at the language level (QuickCheck-style).
	•	Determinism checker for workflows: fails if reading non-deterministic global state.

⸻

10) Tooling experience (crucial for adoption)
    •	LSP server: completion, go-to-def, hover with effect signatures, quick-fixes for missing capabilities.
    •	asterpm: build, test, run workers, deploy.
    •	aster doc: renders contracts, effect graphs, retry policies, and compensation chains as diagrams.
    •	Scaffolds from prompts: “create durable order workflow with stock reservation and Stripe capture” → generates typed skeleton + tests.

⸻

11) Performance expectations
    •	Keep the core deterministic path allocation-light.
    •	Use persisted state deltas vs snapshots (configurable).
    •	Coalesce timers; batch I/O with idempotency keys.
    •	Target: p50 step scheduling < 10 ms, 1M steps/day/node on modest hardware.

⸻

12) Comparative landscape
    •	Temporal/Cadence + Java/Kotlin: powerful, but the language is general-purpose; workflow intent isn’t first-class. aster makes workflows the primitive with effect checking.
    •	Nextflow/Airflow/Prefect: great for data/ETL, weaker on typed domain modeling and unified interop.
    •	BPMN/DSLs/YAML: verbose, hard to validate, weak type systems.
    •	Kotlin/Scala: wonderful languages, but you still hand-roll durability, retries, compensations, and effect discipline.

⸻

13) Minimal syntax sketch (illustrative)

module shop.orders

type Money = Decimal(precision=18, scale=2)

record Order {
id: UUID
items: List[Item]
total: Money
email: Email
state: State
}

enum State { Created, Reserved, Pending, Cancelled, Completed }

capabilities uses [Sql, Http["https://payments.example"], Email, Time]

fn reserveStock(o: Order) -> Result[Reserved, StockError] uses [Sql]
fn charge(o: Order, amount: Money) -> Result[Receipt, PaymentError] uses [Http]

workflow Fulfill(o: Order) uses [Sql, Email, Time, Http] {
on start:
ensure o.total >= 0

step reserve = retry(max=5, backoff=exp(500ms)) {
match reserveStock(o) {
Ok(_) => continue
Err(e) => { notifySupport(e, o); set o.state = Pending; stop }
}
}

step pay = {
let r = charge(o, o.total)
match r {
Ok(rcpt) => set o.state = Completed
Err(_) => compensate reserve; set o.state = Cancelled
}
}

await Time.after(24h)
if o.state == Pending:
remind(o.email)
}


⸻

14) Roadmap (practical & staged)

Phase 0 – Spec & skeleton (2–4 weeks)
•	Language reference v0.1 (types, modules, effects, workflows).
•	Grammar + parser + AST; error model; pretty-printer.

Phase 1 – Type & effect checker (4–8 weeks)
•	Name/overload resolution; generics; ADTs; pattern match exhaustiveness.
•	Capability/effect checking with simple policies.

Phase 2 – Runtime bootstrap (6–10 weeks)
•	IR + interpreter (fast feedback).
•	JVM backend via transpile-to-Java + Quarkus workers.
•	Minimal durable engine (can wrap Temporal to accelerate).

Phase 3 – Tooling & interop (6–8 weeks)
•	LSP, asterpm, doc generator, OpenTelemetry hooks.
•	Vault/SQL/HTTP capability packs.

Phase 4 – AI-assisted authoring (ongoing)
•	CNL templates; verifier that rejects non-compiling LLM output.
•	Provenance ledger; policy guards for agentic steps.

Phase 5 – Hardening & enterprise
•	Property-based testing; determinism checker; migration tooling.
•	Performance tuning; upgrade & rollback semantics.

⸻

15) Risks & anti-goals
    •	Anti-goal: “write English and magic happens.” Avoid unconstrained NL → code.
    •	Risk: capability creep; keep the core small and orthogonal.
    •	Risk: fragmentation across backends; pick JVM first and nail it.
    •	Risk: runtime complexity; leverage proven primitives (event sourcing, outbox/inbox) instead of inventing new ones.

⸻

16) Concrete next steps for you
    1.	Freeze the core spec (10–12 pages): syntax, types, effects, workflow constructs, retry/compensation.
    2.	Decide syntax style (indentation vs braces) and never mix.
    3.	Publish a reference capability pack (Http, Sql(PostgreSQL), Time, Email, Secrets(Vault)).
    4.	Implement interpreter first (fast compile-run cycle), then Java transpiler.
    5.	Pick a durable substrate (Temporal to start). Generate workers + stubs from aster code.
    6.	Ship 3 compelling samples:
          •	Order fulfillment saga (with compensation).
          •	Data pipeline with retries and idempotency.
          •	Human-in-the-loop AI step with budget guard and prompt provenance.
    7.	Developer ergonomics: LSP, scaffolds, great errors, docs with effect graphs.
