# Aster Language: Domain-Focused Implementation Roadmap

**Version:** 1.0
**Date:** 2025-09-30
**Team Size:** 2-3 senior engineers
**Timeline:** 18-24 months to production-ready v1.0

---

## Executive Summary

### Vision
Position Aster as the **definitive language for auditable, business-critical workflows** in regulated industries (FinTech, HealthTech, InsurTech). Aster uniquely combines human-readable syntax, type-safe effect tracking, and durable execution semantics to bridge the gap between business requirements and production code.

### Strategic Positioning
**We are NOT building a general-purpose language.** We are building the **best tool for five specific domains** where existing solutions fail:

1. **Business Rules & Policy Engines** — Replace Drools, BPMN, and untyped YAML with auditable, versionable code
2. **Workflow Orchestration** — Make Temporal/Cadence workflows first-class language constructs with compiler-enforced compensations
3. **Regulatory/Compliance Documentation** — Code that IS the documentation (GDPR, HIPAA, SOC2 compliance)
4. **AI-Assisted Code Generation** — Constrained CNL syntax enables safe LLM code generation with provenance tracking
5. **Domain-Specific Automation** — Enable subject-matter experts (doctors, accountants, loan officers) to read and validate business logic

### Success Metrics (24-month targets)
- **Adoption:** 10+ production deployments in regulated industries
- **Readability:** 80%+ comprehension by non-programmers in user studies (vs. <30% for Kotlin/Java)
- **Audit Compliance:** 100% of generated code passes automated compliance checks (vs. manual audits)
- **Performance:** <100ms p99 latency for policy evaluation; <10ms workflow step scheduling
- **Ecosystem:** 5+ domain-specific libraries (`aster-finance`, `aster-healthcare`, etc.) with 50+ contributors

### Core Differentiators
| Feature | Aster | Temporal | Drools | BPMN |
|---------|-------|----------|--------|------|
| **Human-readable syntax** | ✅ CNL | ❌ Go/Java | ❌ XML | ❌ Visual only |
| **Type-safe effects** | ✅ Compiler-enforced | ❌ Runtime only | ❌ None | ❌ None |
| **Compensations as syntax** | ✅ `compensate` keyword | ❌ Manual | ❌ Manual | ⚠️ Limited |
| **Audit trail built-in** | ✅ Provenance tracking | ⚠️ Via SDK | ❌ External | ❌ External |
| **AI code generation** | ✅ Constrained CNL | ❌ Unconstrained | ❌ N/A | ❌ N/A |
| **Non-programmer readable** | ✅ 80%+ | ❌ <10% | ❌ <5% | ⚠️ 40% |

---

## Architecture Overview

### System Architecture (Layered)

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ Vaadin UI    │  │ VS Code LSP  │  │ CLI Tools    │           │
│  │ (Policy Ed.) │  │ (Dev IDE)    │  │ (Build/Test) │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    COMPILER TOOLCHAIN (Node.js/TS)              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ CNL → Canonicalize → Lex → Parse → AST → Core IR         │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ Type Checker │  │ Effect Check │  │ Audit Gen    │           │
│  │ (Inference)  │  │ (Capability) │  │ (Provenance) │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CODE GENERATION LAYER                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ JVM Bytecode │  │ Truffle AST  │  │ Temporal SDK │           │
│  │ (ASM Emitter)│  │ (Interpreter)│  │ (Workflow)   │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    RUNTIME LAYER (Quarkus + GraalVM)            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Durable Workflow Engine (Event-Sourced State Machine)    │   │
│  │  - PostgreSQL Event Store                                │   │
│  │  - Saga Coordinator (Compensations)                      │   │
│  │  - Retry/Backoff Scheduler                               │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐         │
│  │ Policy Engine│  │ Audit Logger │  │ Metrics/Trace  │         │
│  │ (Rules Eval) │  │ (Compliance) │  │ (OpenTelemetry)│         │
│  └──────────────┘  └──────────────┘  └────────────────┘         │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    INTEGRATION LAYER                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ PostgreSQL   │  │ Kafka/NATS   │  │ REST/gRPC    │           │
│  │ (Event Store)│  │ (Messaging)  │  │ (External)   │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

### Core Components

#### 1. Enhanced Compiler (Node.js/TypeScript)
**Current State:** CNL → Core IR → JVM bytecode
**Required Enhancements:**
- **Effect System Enforcement** (Phase 0, Completed): Compile-time errors for missing effects; minimal lattice IO ⊒ CPU
- **Workflow Semantics** (Phase 2): Add `workflow`, `compensate`, `retry` as first-class constructs
- **Audit Metadata** (Phase 2): Attach provenance, version, and compliance tags to all IR nodes
- **Domain DSL Support** (Phase 3): Plugin system for domain-specific syntax extensions

#### 2. Durable Workflow Runtime (Quarkus + GraalVM)
**Current State:** None (Truffle interpreter is experimental)
**Required Components:**
- **Event-Sourced State Machine** (Phase 2): PostgreSQL-backed event store with snapshots
- **Saga Coordinator** (Phase 2): Automatic compensation execution on failure
- **Scheduler** (Phase 2): Durable timers, retry policies, exponential backoff
- **Polyglot Interop** (Phase 3): Call Java/Kotlin/Scala libraries via GraalVM

#### 3. Visual Tooling (Vaadin)
**Current State:** None
**Required Tools:**
- **Policy Editor** (Phase 3): Drag-and-drop rule builder for non-programmers
- **Workflow Designer** (Phase 3): Visual saga/compensation graph editor
- **Audit Dashboard** (Phase 4): Real-time compliance monitoring and reporting

#### 4. LSP Server (Node.js/TypeScript)
**Current State:** Feature-complete for basic editing
**Required Enhancements:**
- **Domain-Specific Completions** (Phase 3): Context-aware suggestions for finance/healthcare/insurance
- **Compliance Linting** (Phase 3): Real-time checks for GDPR/HIPAA/SOC2 violations
- **AI Code Actions** (Phase 4): LLM-powered refactoring and policy generation

---
## Language Semantics Addendum: Effects, Determinism, Idempotency

### Minimal Effect Lattice and Capability Model
- Effects are tracked as rows on functions and workflows: `with { CPU | IO[Capability*] }`.
- Base lattice: `∅ ⊑ CPU ⊑ IO[∗]` where `CPU` denotes pure CPU-bound computation; `IO[...]` denotes capability-scoped side-effects.
- Capabilities are parameterized:
  - `Http[method: GET|POST|..., retry: Policy, idempotent: Bool]`
  - `Sql[db: DSN, tx: ReadOnly|ReadWrite]`
  - `Time` (durable timers for workflows)
  - `Files[path: Pattern]`
  - `Secrets[scope: VaultPath]`
  - `AiModel[name: Text, budget: Money]`
- Subsumption/Join:
  - `IO[A] ⊔ IO[B] = IO[A ∪ B]`; `CPU ⊔ IO[X] = IO[X]`.
  - `IO[X] ⊑ IO[Y]` iff `X ⊆ Y` (capability set inclusion incl. compatible params).
- Enforcement & diagnostics (implemented):
  - Missing effect annotations are compile-time errors. IO subsumes CPU (declaring `@io` satisfies CPU work).
  - Superfluous annotations: `@io` with only CPU-like work → info; `@cpu` with no CPU-like work → warning.
  - Detection currently uses a small prefix registry (`src/config/effects.ts`).
- CNL-first capability syntax (accepted; bracket sugar implemented; enforcement behind flag):
  - You can write either of the following (equivalent):
    - `It performs io with Http and Sql and Time.`
    - `It performs io [Http, Sql, Time].`
  - Formatter normalizes capability lists to the bracket form in example files.
  - Golden tests cover both forms (AST/CORE parse preservation and gated enforcement).
- See also: docs/reference/effects-capabilities.md for the capability-parameterized effects design and feature flags.

- CI notes:
  - Golden runner executes both CNL and bracket forms for parse/Core and capability-enforcement diagnostics.
  - Capability enforcement is enabled for golden diagnostics via ASTER_CAP_EFFECTS_ENFORCE=1 inside scripts/golden.ts.
  - Example formatter preserves capability lists and normalizes to bracket form to keep tests stable.
  - Relevant scripts: `npm test`, `npm run ci`, `npm run ci:strict`.
  - Placeholder variables during refactors: if you must keep a temporary unused binding, add an inline override on the declaration line to satisfy CI linting:
    - `// eslint-disable-next-line @typescript-eslint/no-unused-vars` (see example in `src/parser.ts`)
    - Keep this strictly temporary; remove the placeholder or wire it up before merging.




- Effect polymorphism (minimal):
  - Functions may quantify over an effect row variable `E` appearing only positively:
    - Example: `To map with f: T -> U with E, xs: List of T, produce List of U with E`.
- Must-handle effects:
  - Workflows emitting `Time`, `Http` with `retry`, or any capability marked `compensable: true` must provide a compensation step; enforced statically.

### Compensations as Types
- For any step `Step<T,E>` in a workflow, define a dual `Compensate<T>` that reverses external effects where feasible.
- The compiler enforces presence of `Compensate<T>` where the step’s capability set contains compensable effects (e.g., `Sql[ReadWrite]`, `Http[POST]`).

### Determinism Contract (Workflows)
- Prohibited implicit sources of nondeterminism inside workflow logic: wall-clock time, random, UUIDs, environment/process state, unordered iteration over maps/sets.
- All nondeterminism must flow through capability wrappers:
  - `DeterministicTime.now()`, `Uuid.v4()`, `Rand.next()`
  - Each call records a decision in the event log so replay reproduces the same value.
- Deterministic replay requirement:
  - Re-executing a workflow from its event log must yield identical state transitions and outputs.
  - CI includes replay tests with injected failures (crash before/after persist, timer wakeups).

### Idempotency & Exactly-Once Effects
- Outbound side-effects (HTTP, messages) carry an `Idempotency-Key` produced by the runtime; the event store deduplicates by `(endpoint, key)`.
- Inbox/outbox patterns:
  - Outbox: persist intent before external call; on retry, resend using same key.
  - Inbox: deduplicate inbound events/commands by source+key; persist result mapping.
- Runtime provides helpers and enforces usage for capabilities marked `atLeastOnce: true`.

### PII/Units/Refinements (Minimal)
- Add labeled types: `PII<Text>` (with optional tags: email, ssn). Compiler enforces redaction in logs unless explicitly unwrapped.
- Units: lightweight phantom units for numbers (e.g., `Millis`, `Days`, `USD` via `Money`). Arithmetic only allowed on compatible units.
- Refinements: bounded integers (e.g., `CreditScore: Int in 300..850`) enforced at construction and via type-directed validators.

### Policy Explanation Traces
- Policy evaluation produces an optional `Explanation`:
  - `rulePath: List of RuleId`, `salientInputs: Map<Text, Value>`, `outcome: Decision`.
- API: `POST /evaluate?explain=true` returns decision + explanation; used for audits and SME debugging.


## Phase-by-Phase Implementation Plan

### Phase 0: Foundation Hardening (Months 1-3, 2 engineers)
**Goal:** Stabilize existing projct and establish production-grade infrastructure

#### Deliverables
- [ ] **Effect System Enforcement**
  - [x] Upgrade effect checker from warnings to errors
  - [ ] Implement effect inference (track effects through call graph)
  - [ ] Add effect polymorphism (`fn map<E>(f: T -> U with E): List<U> with E`)
  - [x] Golden tests for effect violations
  - **Acceptance:** All examples type-check with strict effect enforcement; CI fails on effect violations
  - **Effort:** 4 person-weeks

- [ ] **Complete Truffle Interpreter**
  - [ ] Implement all Core IR nodes (currently ~50% coverage)
  - [ ] Add stdlib runtime (Text, List, Map, Result, Maybe operations)
  - [ ] Benchmark vs. JVM bytecode (target: within 2x after warmup)
  - **Acceptance:** All golden tests pass on Truffle; performance within 2x of JVM
  - **Effort:** 6 person-weeks

- [ ] **Production Build Pipeline**
  - [ ] Deterministic builds (reproducible bytecode)
  - [ ] Native-image support (GraalVM AOT compilation)
  - [ ] Docker images for runtime (Quarkus + PostgreSQL)
  - [ ] CI/CD pipeline (GitHub Actions: build, test, deploy)
  - **Acceptance:** `docker run aster/runtime` starts workflow engine; native binary <50MB
  - **Effort:** 3 person-weeks

- [ ] **Determinism Contract (Workflows)**
  - [ ] Provide `DeterministicTime`, `Uuid`, `Rand` facades; forbid implicit sources in workflows
  - [ ] Record all nondeterministic decisions to event log; add replay runner
  - [ ] Failure-injection tests (crash before/after persist, timer fire/retry)
  - **Acceptance:** Replaying from event log yields identical decisions and final state across 100 randomized failure scenarios
  - **Effort:** 3 person-weeks

- [ ] **Idempotency & Exactly-Once Helpers**
  - [ ] Outbox pattern for external calls with persisted `Idempotency-Key`
  - [ ] Inbox deduplication for inbound events/commands
  - [ ] SDK helpers for `Http`, `Kafka/NATS` capabilities
  - **Acceptance:** End-to-end test shows zero duplicates under at-least-once delivery and retries
  - **Effort:** 3 person-weeks

- [ ] **Tamper-Evident Audit Log**
  - [ ] Hash-chain audit entries with per-deployment signing key
  - [ ] Export verifiable audit bundles; optional transparency log anchoring (Sigstore/Rekor)
  - **Acceptance:** Verifier tool detects any alteration in `policy_audit` records and proofs validate
  - **Effort:** 2 person-weeks

- [ ] **PII-Aware Logging Guardrails**
  - [ ] Introduce `PII<Text>` with tags; block logging unless redacted or explicitly unwrapped
  - [ ] LSP diagnostics: flag potential PII leakage
  - **Acceptance:** CI policy denies merges that log PII without redaction; sample apps pass
  - **Effort:** 2 person-weeks


- [ ] **Documentation & Onboarding**
  - [ ] Language specification (formal grammar, type rules, effect rules)
  - [ ] Getting started guide (install, first policy, first workflow)
  - [ ] API reference (stdlib, runtime, interop)
  - **Acceptance:** New developer can build and deploy a policy in <1 hour
  - **Effort:** 2 person-weeks

**Total Effort:** 15 person-weeks (3 months with 2 engineers)
**Risk Mitigation:**
- Effect system complexity → Start with simple inference, defer polymorphism to Phase 1
- Truffle performance → Focus on correctness first, optimize in Phase 1
- Native-image issues → Keep reflection-free design, test early and often

---

### Phase 1: Business Rules & Policy Engine (Months 4-6, 2-3 engineers)
**Goal:** Ship production-ready policy engine for FinTech/InsurTech use cases

#### Deliverables
- [ ] **Policy Engine Runtime (Quarkus)**
  - [ ] REST API for policy evaluation (`POST /evaluate`)
  - [ ] Policy versioning (immutable deployments, rollback support)
  - [ ] Audit logging (every evaluation logged with input/output/decision)
  - [ ] Performance: <10ms p50, <100ms p99 for policy evaluation
  - [ ] Explanation traces (`explain=true`) return rule path and salient inputs
  - [ ] PII-aware logging: redact or block by default based on `PII<Text>` labels

  - **Acceptance:** Deploy loan approval policy; handle 1000 req/s with full audit trail
  - **Effort:** 8 person-weeks

- [ ] **Domain Library: `aster-finance`**
  - [ ] Types: `Money`, `CreditScore`, `LoanApplication`, `RiskLevel`
  - [ ] Policies: `approveLoan`, `calculateInterestRate`, `assessRisk`
  - [ ] Compliance: Built-in TILA (Truth in Lending Act) checks
  - [ ] Examples: Mortgage approval, credit card limit, fraud detection
  - **Acceptance:** 3 real-world financial policies implemented and tested
  - **Effort:** 6 person-weeks

- [ ] **Visual Policy Editor (Vaadin)**
  - [ ] Rule builder UI (if-then-else, match-case, comparisons)
  - [ ] Live preview (test policy with sample inputs)
  - [ ] Export to CNL (round-trip: UI → CNL → UI)
  - **Acceptance:** Non-programmer can build a loan approval policy in <30 minutes
  - **Effort:** 10 person-weeks

- [ ] **Testing & Validation Framework**
  - [ ] Property-based testing (QuickCheck-style for policies)
  - [ ] Coverage analysis (which rules are exercised)
  - [ ] Mutation testing (detect weak policies)
  - **Acceptance:** 100% branch coverage on all example policies
  - **Effort:** 4 person-weeks

**Total Effort:** 28 person-weeks (6 weeks with 3 engineers, or 9 weeks with 2)
**Success Criteria:**
- Deploy to 1 pilot customer (FinTech startup or InsurTech)
- Process 1M+ policy evaluations in production
- Zero compliance violations in audit

**Risk Mitigation:**
- Performance bottlenecks → Profile early, optimize hot paths (JIT-friendly code)
- UI complexity → Start with simple form-based editor, defer drag-and-drop to Phase 3
- Customer adoption → Partner with 1-2 early adopters, co-develop features

---

### Phase 2: Workflow Orchestration (Months 7-12, 3 engineers)
**Goal:** Ship durable workflow engine with saga/compensation support

#### Deliverables
- [ ] **Workflow Language Extensions**
  - [ ] Syntax: `workflow`, `step`, `compensate`, `retry`, `timeout`
  - [ ] Core IR: `Workflow`, `Step`, `Compensate`, `Retry` nodes
  - [ ] Type system: Workflow types (`Workflow<T, E>` with effects)
  - [ ] Examples: Order fulfillment, user onboarding, payment processing
  - **Acceptance:** All workflow examples compile and type-check
  - **Effort:** 6 person-weeks

- [ ] **Durable Execution Runtime (Quarkus + PostgreSQL)**
  - [ ] Event-sourced state machine (append-only event log)
  - [ ] Saga coordinator (automatic compensation on failure)
  - [ ] Durable timers (`await Time.after(3 days)`)
  - [ ] Retry policies (exponential backoff, jitter, max attempts)
  - [ ] Idempotency (deduplication via request IDs)
  - **Acceptance:** Workflow survives process crash and resumes from last checkpoint
  - **Effort:** 12 person-weeks

- [ ] **Temporal Integration (Optional)**
  - [ ] Code generator: Aster workflow → Temporal Go/Java code
  - [ ] Interop: Call Temporal activities from Aster
  - [ ] Migration tool: Import existing Temporal workflows
  - **Acceptance:** Run Aster workflow on Temporal Cloud
  - **Effort:** 8 person-weeks

- [ ] **Domain Library: `aster-ecommerce`**
  - [ ] Workflows: Order fulfillment, payment processing, inventory management
  - [ ] Compensations: Cancel order, refund payment, release inventory
  - [ ] Integrations: Stripe, Shopify, AWS S3
  - **Acceptance:** Deploy e-commerce workflow to production
  - **Effort:** 10 person-weeks

**Total Effort:** 36 person-weeks (12 weeks with 3 engineers)
**Success Criteria:**
- Deploy to 2 pilot customers (e-commerce or SaaS)
- Process 100K+ workflow executions in production
- <1% failure rate (excluding expected business errors)

**Risk Mitigation:**
- Runtime complexity → Start with simple event store, defer snapshots/sharding to Phase 3
- Temporal integration → Make optional, focus on native runtime first
- Performance → Target 100 workflows/sec initially, optimize later

---

### Phase 3: Compliance & AI-Assisted Generation (Months 13-18, 3 engineers)
**Goal:** Enable regulatory compliance and LLM-powered code generation

#### Deliverables
- [ ] **Compliance Framework**
  - [ ] Audit trail generation (every decision logged with provenance)
  - [ ] Version control integration (Git-based policy versioning)
  - [ ] Compliance reports (GDPR Article 17, HIPAA 164.308, SOC2 CC6.1)
  - [ ] Rollback/replay (rerun historical decisions with old policy versions)
  - **Acceptance:** Generate SOC2 compliance report from production audit logs
  - **Effort:** 8 person-weeks

- [ ] **Domain Library: `aster-healthcare`**
  - [ ] Types: `Patient`, `Diagnosis`, `Treatment`, `Prescription`
  - [ ] Policies: Clinical decision support, drug interaction checks, HIPAA consent
  - [ ] Compliance: Built-in HIPAA privacy rules
  - [ ] Examples: Treatment recommendation, prescription validation, consent management
  - **Acceptance:** Deploy clinical decision support system to pilot hospital
  - **Effort:** 10 person-weeks

- [ ] **AI Code Generation Pipeline**
  - [ ] LLM prompt templates (policy generation, workflow scaffolding)
  - [ ] Constrained generation (CNL grammar as LLM constraint)
  - [ ] Type-checking validation (reject invalid LLM output)
  - [ ] Provenance tracking (attach prompt/model/timestamp to generated code)
  - [ ] Human-in-the-loop review (diff UI for LLM suggestions)
  - **Acceptance:** LLM generates valid policy from English description 80%+ of the time
  - **Effort:** 12 person-weeks

- [ ] **LSP Enhancements for Compliance**
  - [ ] Real-time compliance linting (GDPR/HIPAA violations highlighted)
  - [ ] Code actions: "Add GDPR consent check", "Log HIPAA access"
  - [ ] Hover: Show compliance requirements for each policy rule
  - **Acceptance:** LSP warns about missing consent check in real-time
  - **Effort:** 6 person-weeks

**Total Effort:** 36 person-weeks (12 weeks with 3 engineers)
**Success Criteria:**
- Pass external SOC2 audit using Aster-generated compliance reports
- LLM generates 100+ policies with 80%+ correctness
- Deploy to 1 healthcare pilot customer

**Risk Mitigation:**
- LLM hallucinations → Strict type-checking, human review required
- Compliance complexity → Partner with compliance experts, validate against real audits
- Healthcare regulations → Start with non-critical use cases (e.g., appointment scheduling)

---

### Phase 4: Ecosystem & Scaling (Months 19-24, 3 engineers)
**Goal:** Build community, scale to enterprise, optimize performance

#### Deliverables
- [ ] **Package Management & Plugin System**
  - [ ] Package registry (`aster-registry.io`)
  - [ ] Dependency resolution (semantic versioning)
  - [ ] Plugin API (custom effects, capabilities, code generators)
  - [ ] Community contribution guidelines
  - **Acceptance:** 5+ third-party domain libraries published
  - **Effort:** 10 person-weeks

- [ ] **Domain Libraries (Community-Driven)**
  - [ ] `aster-insurance`: Underwriting, claims processing, fraud detection
  - [ ] `aster-legal`: Contract review, compliance checks, e-discovery
  - [ ] `aster-logistics`: Route optimization, inventory management, delivery tracking
  - **Acceptance:** Each library has 3+ real-world examples and 10+ contributors
  - **Effort:** 15 person-weeks (seed libraries, then community takes over)

- [ ] **Performance Optimization**
  - [ ] JIT compilation (GraalVM native-image with PGO)
  - [ ] Parallel policy evaluation (multi-threaded rule engine)
  - [ ] Workflow sharding (distribute across multiple nodes)
  - [ ] Caching (memoize policy results, deduplicate events)
  - **Acceptance:** 10x throughput improvement (10K policies/sec, 1K workflows/sec)
  - **Effort:** 12 person-weeks

- [ ] **Enterprise Features**
  - [ ] Multi-tenancy (isolated policy/workflow namespaces)
  - [ ] RBAC (role-based access control for policy editing)
  - [ ] High availability (active-active runtime with leader election)
  - [ ] Monitoring (Prometheus metrics, Grafana dashboards)
  - **Acceptance:** Deploy to Fortune 500 customer with 99.9% SLA
  - **Effort:** 15 person-weeks

**Total Effort:** 52 person-weeks (17 weeks with 3 engineers)
**Success Criteria:**
- 10+ production deployments across 3+ industries
- 50+ community contributors
- 99.9% uptime in production
- <10ms p99 latency for policy evaluation

**Risk Mitigation:**
- Community adoption → Invest in docs, tutorials, conference talks
- Performance bottlenecks → Profile continuously, optimize incrementally
- Enterprise sales → Partner with consulting firms, offer professional services

---

## Technology Stack Integration Guide

### 1. GraalVM + Native-Image
**Purpose:** Fast startup, low memory footprint, polyglot interop
**Usage:**
- **Compiler:** Run Aster compiler as native binary (instant startup for CLI)
- **Runtime:** AOT-compile workflow engine for <100ms startup (vs. 2-3s JVM)
- **Interop:** Call Java/Kotlin/Scala libraries from Aster policies

**Integration Steps:**
1. Add `native-image` configuration to `build.gradle.kts`
2. Generate reflection metadata for runtime classes
3. Test native binary with all examples
4. Benchmark startup time and memory usage

**Acceptance Criteria:**
- Native binary <50MB
- Startup time <100ms
- Memory usage <50MB for simple policies

---

### 2. Quarkus
**Purpose:** Build durable workflow runtime and REST APIs
**Usage:**
- **Workflow Engine:** Event-sourced state machine with PostgreSQL
- **Policy API:** REST endpoints for policy evaluation
- **Admin UI:** Vaadin-based policy editor and audit dashboard

**Integration Steps:**
1. Create Quarkus project with Hibernate Reactive + PostgreSQL
2. Implement event store (append-only log with snapshots)
3. Add REST API for workflow execution and policy evaluation
4. Integrate with Vaadin for admin UI

**Acceptance Criteria:**
- <10ms p50 latency for policy evaluation
- <100ms p99 latency for workflow step scheduling
- 1000+ req/s throughput on single node

---

### 3. Vaadin
**Purpose:** Build visual policy editors and workflow designers
**Usage:**
- **Policy Editor:** Drag-and-drop rule builder for non-programmers
- **Workflow Designer:** Visual saga/compensation graph editor
- **Audit Dashboard:** Real-time compliance monitoring

**Integration Steps:**
1. Create Vaadin Flow project (Java + TypeScript)
2. Build policy editor UI (form-based, then drag-and-drop)
3. Integrate with Aster compiler (CNL ↔ UI round-trip)
4. Add live preview (test policy with sample inputs)

**Acceptance Criteria:**
- Non-programmer can build loan approval policy in <30 minutes
- UI    CNL    UI round-trip preserves semantics
- Live preview shows policy results in <1 second


### 7. Tamper-Evident Audit Logging
**Purpose:** Make audit trails verifiable and tamper-evident for regulators

**Design:**
- Hash-chain each `policy_audit` record: `h_i = H(h_{i-1} || record_i)`; store `h_i` alongside record
- Sign daily checkpoints with deployment key; rotate keys and keep public certs
- Optional: Anchor daily checkpoint to a transparency log (Sigstore/Rekor)

**Integration Steps:**
1. Extend `policy_audit` schema with `hash BYTEA`, `prev_hash BYTEA`, `signature BYTEA`
2. Implement verifier tool to recompute chain and verify signatures
3. Add exporter to generate verifiable audit bundles (JSON + proofs)

**Acceptance Criteria:**
- Any mutation in historical audit entries is detected by the verifier
- Audit bundles verify on a clean machine with only public certs


---

### 4. Node.js + TypeScript
**Purpose:** Compiler toolchain, LSP server, build tools
**Usage:**
- **Compiler:** CNL → Core IR → JVM bytecode
- **LSP Server:** IDE integration (VS Code, IntelliJ)
- **Build Tools:** CLI, package manager, test runner

**Integration Steps:**
1. Keep existing TypeScript compiler (already production-ready)
2. Add plugin system for domain-specific syntax extensions
3. Enhance LSP with compliance linting and AI code actions
4. Build package manager (`aster-pm`) for domain libraries

**Acceptance Criteria:**
- Compiler processes 1000 LOC in <1 second
- LSP responds to hover/completion in <30ms
- Package manager resolves dependencies in <5 seconds

---

### 5. PostgreSQL (Event Store)
**Purpose:** Durable state persistence for workflows
**Usage:**
- **Event Log:** Append-only table for workflow events
- **Snapshots:** Periodic checkpoints for fast recovery
- **Audit Trail:** Immutable log of all policy decisions

**Schema:**
```sql
CREATE TABLE workflow_events (
  id BIGSERIAL PRIMARY KEY,
  workflow_id UUID NOT NULL,
  event_type TEXT NOT NULL,
  payload JSONB NOT NULL,
  timestamp TIMESTAMPTZ DEFAULT NOW(),
  version INT NOT NULL
);

CREATE TABLE policy_audit (
  id BIGSERIAL PRIMARY KEY,
  policy_name TEXT NOT NULL,
  policy_version TEXT NOT NULL,
  input JSONB NOT NULL,
  output JSONB NOT NULL,
  decision TEXT NOT NULL,
  timestamp TIMESTAMPTZ DEFAULT NOW()
);
```

**Acceptance Criteria:**
- 10K+ events/sec write throughput
- <10ms read latency for recent events
- 100% durability (no data loss on crash)

---

### 6. Kafka/NATS (Optional, Phase 4)
**Purpose:** Distributed workflow coordination
**Usage:**
- **Event Bus:** Publish workflow events to external systems
- **Saga Coordination:** Distributed transactions across microservices
- **Scalability:** Horizontal scaling of workflow engine

**Integration Steps:**
1. Add Kafka producer to workflow engine
2. Publish events on workflow state changes
3. Consume events from external systems (e.g., payment gateway)
4. Implement exactly-once semantics (idempotency keys)

**Acceptance Criteria:**
- 100K+ events/sec throughput
- <100ms end-to-end latency
- Zero duplicate events (exactly-once delivery)

---

## Domain-Specific Feature Matrix

### 1. Business Rules & Policy Engines

| Feature | Requirement | Implementation | Priority |
|---------|-------------|----------------|----------|
| **Human-readable syntax** | Non-programmers can read policies | CNL with `If`, `Match`, `Return` | P0 (Phase 1) |
| **Type safety** | Catch errors at compile-time | Strong typing + inference | P0 (Phase 0) |
| **Versioning** | Immutable policy deployments | Git-based versioning + API | P0 (Phase 1) |
| **Audit logging** | Every decision logged | PostgreSQL audit table | P0 (Phase 1) |
| **Performance** | <10ms p50, <100ms p99 | JIT compilation + caching | P1 (Phase 4) |
| **Visual editor** | Drag-and-drop rule builder | Vaadin UI | P1 (Phase 1) |
| **Testing** | Property-based + mutation testing | QuickCheck-style framework | P1 (Phase 1) |

**Example Use Cases:**
- **Loan Approval:** Credit score, income, debt-to-income ratio → approve/deny
- **Fraud Detection:** Transaction amount, location, velocity → flag/allow
- **Insurance Underwriting:** Age, health, occupation → premium calculation

---

### 2. Workflow Orchestration

| Feature | Requirement | Implementation | Priority |
|---------|-------------|----------------|----------|
| **Durable execution** | Survive process crashes | Event-sourced state machine | P0 (Phase 2) |
| **Compensations** | Automatic rollback on failure | `compensate` keyword + saga coordinator | P0 (Phase 2) |
| **Retries** | Exponential backoff, jitter | `retry` policy in syntax | P0 (Phase 2) |
| **Timers** | Durable delays (days/weeks) | PostgreSQL-backed scheduler | P0 (Phase 2) |
| **Idempotency** | Deduplicate requests | Request ID tracking | P0 (Phase 2) |
| **Visual designer** | Drag-and-drop workflow graph | Vaadin UI | P1 (Phase 3) |
| **Temporal integration** | Run on Temporal Cloud | Code generator | P2 (Phase 2) |

**Example Use Cases:**
- **Order Fulfillment:** Reserve inventory → charge card → ship → compensate if any step fails
- **User Onboarding:** Create account → send email → wait 3 days → provision resources
- **Payment Processing:** Authorize → capture → settle → refund if dispute

---

### 3. Regulatory/Compliance Documentation

| Feature | Requirement | Implementation | Priority |
|---------|-------------|----------------|----------|
| **Provenance tracking** | Every line of code has origin | Metadata in Core IR | P0 (Phase 3) |
| **Audit reports** | Generate compliance docs | GDPR/HIPAA/SOC2 templates | P0 (Phase 3) |
| **Version control** | Git-based policy history | Git integration | P0 (Phase 1) |
| **Rollback/replay** | Rerun old decisions | Event store replay | P0 (Phase 2) |
| **Compliance linting** | Real-time violation detection | LSP diagnostics | P1 (Phase 3) |
| **External audit** | Third-party verification | Export to PDF/HTML | P1 (Phase 3) |

**Example Use Cases:**
- **GDPR Right-to-Erasure:** Delete user data across all systems
- **HIPAA Access Logs:** Track who accessed patient records
- **SOC2 Access Control:** Verify role-based permissions

---

### 4. AI-Assisted Code Generation

| Feature | Requirement | Implementation | Priority |
|---------|-------------|----------------|----------|
| **Constrained generation** | LLM outputs valid CNL | Grammar-based prompts | P0 (Phase 3) |
| **Type-checking** | Reject invalid LLM output | Compiler validation | P0 (Phase 3) |
| **Provenance** | Track prompt/model/timestamp | Metadata in IR | P0 (Phase 3) |
| **Human review** | Diff UI for suggestions | VS Code extension | P0 (Phase 3) |
| **Fine-tuning** | Domain-specific LLM | Train on Aster corpus | P2 (Phase 4) |

**Example Use Cases:**
- **Policy Generation:** "Allow admins to delete any document" → CNL policy
- **Workflow Scaffolding:** "Order fulfillment with Stripe" → Workflow skeleton
- **Migration:** Convert Drools XML to Aster CNL

---

### 5. Domain-Specific Automation

| Feature | Requirement | Implementation | Priority |
|---------|-------------|----------------|----------|
| **Domain libraries** | Finance, healthcare, insurance | `aster-*` packages | P0 (Phase 1-4) |
| **Custom types** | Money, CreditScore, Diagnosis | Stdlib extensions | P0 (Phase 1) |
| **Interop** | Call Java/Kotlin libraries | GraalVM polyglot | P0 (Phase 0) |
| **Visual tools** | Non-programmer UIs | Vaadin editors | P1 (Phase 1-3) |
| **Documentation** | Domain-specific guides | Tutorials + examples | P1 (Phase 1-4) |

**Example Use Cases:**
- **Clinical Decision Support:** Symptoms → diagnosis → treatment
- **Tax Calculation:** Income, deductions → tax owed
- **Loan Approval:** Credit score, income → approve/deny

---

## Extensibility Framework Specification

### Plugin Architecture

#### 1. Domain-Specific Syntax Extensions
**Goal:** Allow domain libraries to add custom keywords and constructs

**API Design:**
```typescript
// aster-finance/src/plugin.ts
export const AsterFinancePlugin: AsterPlugin = {
  name: 'aster-finance',
  version: '1.0.0',

  // Custom keywords
  keywords: {
    'calculate interest': {
      parse: (parser) => parseInterestCalculation(parser),
      lower: (ast) => lowerToCore(ast),
      typecheck: (ctx, node) => checkInterestTypes(ctx, node)
    }
  },

  // Custom types
  types: {
    Money: { kind: 'Decimal', precision: 18, scale: 2 },
    CreditScore: { kind: 'Int', range: [300, 850] }
  },

  // Custom effects
  effects: {
    CreditBureau: { io: true, idempotent: false }
  }
};
```

**Registration:**
```typescript
// aster.config.ts
import { AsterFinancePlugin } from 'aster-finance';

export default {
  plugins: [AsterFinancePlugin],
  strict: true
};
```

---

#### 2. Package Structure
**Standard Layout:**
```
aster-finance/
├── src/
│   ├── plugin.ts          # Plugin registration
│   ├── types.ts           # Domain types (Money, CreditScore, etc.)
│   ├── policies/          # Pre-built policies
│   │   ├── loan-approval.aster
│   │   ├── fraud-detection.aster
│   │   └── risk-assessment.aster
│   ├── workflows/         # Pre-built workflows
│   │   ├── payment-processing.aster
│   │   └── account-opening.aster
│   └── runtime/           # Java runtime helpers
│       └── FinanceInterop.java
├── test/
│   ├── policies.test.ts
│   └── workflows.test.ts
├── docs/
│   ├── README.md
│   └── examples/
├── package.json
└── aster.json             # Package metadata
```

**Package Metadata (`aster.json`):**
```json
{
  "name": "aster-finance",
  "version": "1.0.0",
  "description": "Financial domain library for Aster",
  "keywords": ["finance", "banking", "loans"],
  "dependencies": {
    "aster-core": "^1.0.0"
  },
  "exports": {
    "types": "./src/types.ts",
    "policies": "./src/policies",
    "workflows": "./src/workflows"
  },
  "compliance": {
    "regulations": ["TILA", "FCRA", "ECOA"],
    "certifications": ["SOC2", "PCI-DSS"]
  }
}
```

---

#### 3. Package Manager (`aster-pm`)
**Commands:**
```bash
# Install domain library
aster-pm install aster-finance

# Search packages
aster-pm search healthcare

# Publish package
aster-pm publish

# Update dependencies
aster-pm update
```

**Dependency Resolution:**
- Semantic versioning (SemVer)
- Lock file (`aster-lock.json`)
- Conflict resolution (prefer latest compatible version)

---

#### 4. Community Contribution Model

**Contribution Workflow:**
1. **Fork template:** `aster-pm create aster-mylib --template=domain`
2. **Develop:** Add types, policies, workflows, tests
3. **Test:** `aster-pm test` (runs all tests + compliance checks)
4. **Document:** Add examples and API docs
5. **Publish:** `aster-pm publish` (requires review for official registry)

**Quality Standards:**
- 80%+ test coverage
- All examples must compile and run
- Compliance checks pass (if applicable)
- Documentation includes 3+ real-world examples

**Governance:**
- Core team reviews all official packages
- Community packages allowed in separate registry
- Security audits for packages with >1000 downloads/month

---

## Risk Assessment & Mitigation

### Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Effect system too complex** | Medium | High | Start simple (IO/CPU only), defer polymorphism |
| **Runtime performance** | Medium | High | Profile early, optimize hot paths, use GraalVM PGO |
| **Workflow durability bugs** | High | Critical | Extensive testing, chaos engineering, formal verification |
| **LLM hallucinations** | High | Medium | Strict type-checking, human review required |
| **GraalVM native-image issues** | Medium | Medium | Keep reflection-free, test early and often |
| **PostgreSQL bottlenecks** | Low | Medium | Sharding, read replicas, caching |

---

### Timeline Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Phase 1 delays** | Medium | High | Cut visual editor, ship CLI-only |
| **Phase 2 complexity** | High | Critical | Partner with Temporal, use their runtime initially |
| **Phase 3 compliance gaps** | Medium | High | Hire compliance expert, validate with real audits |
| **Phase 4 community adoption** | High | Medium | Invest in docs, tutorials, conference talks |

---

### Adoption Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Developers reject CNL syntax** | Medium | High | Offer concise mode, emphasize readability for audits |
| **Enterprises prefer established tools** | High | High | Partner with consulting firms, offer migration tools |
| **Regulatory uncertainty** | Low | Critical | Work with legal experts, get certifications |
| **Lack of ecosystem** | High | Medium | Seed 5+ domain libraries, fund community contributors |

---

## Success Metrics & Validation Plan

### Quantitative Metrics (24-month targets)

#### Adoption
- [ ] **10+ production deployments** in regulated industries (FinTech, HealthTech, InsurTech)
- [ ] **1M+ policy evaluations/day** across all deployments
- [ ] **100K+ workflow executions/month** across all deployments
- [ ] **50+ community contributors** (GitHub stars, PRs, packages)

#### Performance
- [ ] **<10ms p50 latency** for policy evaluation
- [ ] **<100ms p99 latency** for workflow step scheduling
- [ ] **1000+ req/s throughput** on single node (policy API)
- [ ] **100+ workflows/sec** on single node (workflow engine)

#### Quality
- [ ] **Zero critical bugs** in production (P0 incidents)
- [ ] **99.9% uptime** for hosted runtime
- [ ] **100% compliance** in external audits (SOC2, HIPAA, etc.)
- [ ] **80%+ test coverage** across all code

#### Ecosystem
- [ ] **5+ domain libraries** (`aster-finance`, `aster-healthcare`, etc.)
- [ ] **100+ example policies** across all domains
- [ ] **50+ example workflows** across all domains
- [ ] **10+ blog posts/talks** by community members

---

### Qualitative Metrics

#### Readability (User Studies)
- [ ] **80%+ comprehension** by non-programmers (vs. <30% for Kotlin/Java)
- [ ] **<30 minutes** to build first policy (non-programmer)
- [ ] **<1 hour** to build first workflow (developer)

#### Developer Experience
- [ ] **<1 hour** to install and run first example
- [ ] **<30ms** LSP response time (hover, completion)
- [ ] **<1 second** compile time for 1000 LOC

#### Compliance
- [ ] **100% audit pass rate** (SOC2, HIPAA, GDPR)
- [ ] **Zero manual audit findings** (all issues caught by compiler)
- [ ] **<1 day** to generate compliance report

---

### Validation Plan

#### Phase 0 (Foundation)
- [ ] All golden tests pass on Truffle and JVM
- [ ] Native binary <50MB, startup <100ms
- [ ] Effect violations fail compilation
- [ ] New developer onboarded in <1 hour

#### Phase 1 (Policy Engine)
- [ ] Deploy to 1 pilot customer (FinTech or InsurTech)
- [ ] Process 1M+ policy evaluations in production
- [ ] Zero compliance violations in audit
- [ ] Non-programmer builds policy in <30 minutes (user study)

#### Phase 2 (Workflows)
- [ ] Deploy to 2 pilot customers (e-commerce or SaaS)
- [ ] Process 100K+ workflow executions in production
- [ ] <1% failure rate (excluding expected business errors)
- [ ] Workflow survives process crash and resumes correctly

#### Phase 3 (Compliance & AI)
- [ ] Pass external SOC2 audit using Aster-generated reports
- [ ] LLM generates 100+ policies with 80%+ correctness
- [ ] Deploy to 1 healthcare pilot customer
- [ ] Zero HIPAA violations in audit

#### Phase 4 (Ecosystem)
- [ ] 10+ production deployments across 3+ industries
- [ ] 50+ community contributors
- [ ] 99.9% uptime in production
- [ ] <10ms p99 latency for policy evaluation

---

## Competitive Differentiation (Detailed)

### vs. Drools (Business Rules)

| Aspect | Drools | Aster | Advantage |
|--------|--------|-------|-----------|
| **Syntax** | XML/DRL (cryptic) | CNL (human-readable) | ✅ 80% comprehension vs. <5% |
| **Type Safety** | Runtime errors | Compile-time errors | ✅ Catch bugs before production |
| **Versioning** | Manual | Git-based | ✅ Automatic audit trail |
| **Performance** | RETE algorithm | JIT-compiled | ⚠️ Similar (both fast) |
| **Tooling** | Eclipse plugin | VS Code + Vaadin | ✅ Modern IDE experience |

**Aster Wins:** Readability, type safety, versioning
**Drools Wins:** Mature ecosystem, enterprise support
**Migration Path:** Import Drools DRL → Convert to Aster CNL (automated tool)

---

### vs. Temporal (Workflows)

| Aspect | Temporal | Aster | Advantage |
|--------|----------|-------|-----------|
| **Syntax** | Go/Java SDK | CNL with `workflow` keyword | ✅ Declarative vs. imperative |
| **Compensations** | Manual | `compensate` keyword | ✅ Compiler-enforced |
| **Effects** | Runtime only | Compile-time checked | ✅ Catch missing IO declarations |
| **Durability** | Proven at scale | New (unproven) | ❌ Temporal more mature |
| **Polyglot** | Go, Java, TypeScript | JVM only (Phase 1) | ❌ Temporal more flexible |

**Aster Wins:** Declarative syntax, compiler-enforced compensations
**Temporal Wins:** Proven at scale, polyglot support
**Integration Path:** Generate Temporal code from Aster workflows (Phase 2)

---

### vs. BPMN (Visual Workflows)

| Aspect | BPMN | Aster | Advantage |
|--------|------|-------|-----------|
| **Syntax** | Visual only | CNL + visual | ✅ Code-first with visual option |
| **Type Safety** | None | Compile-time | ✅ Catch errors early |
| **Version Control** | XML (hard to diff) | Git-friendly CNL | ✅ Readable diffs |
| **Execution** | Camunda/Activiti | Native runtime | ⚠️ Similar performance |
| **Tooling** | Camunda Modeler | Vaadin designer | ⚠️ Similar UX |

**Aster Wins:** Type safety, version control, code-first approach
**BPMN Wins:** Industry standard, visual-first
**Migration Path:** Import BPMN XML → Convert to Aster workflows

---

### vs. Low-Code Platforms (Mendix, OutSystems)

| Aspect | Low-Code | Aster | Advantage |
|--------|----------|-------|-----------|
| **Syntax** | Visual only | CNL + visual | ✅ Code-first flexibility |
| **Vendor Lock-In** | High | None (open-source) | ✅ Portable to any JVM |
| **Customization** | Limited | Full (Turing-complete) | ✅ No artificial limits |
| **Performance** | Slow (interpreted) | Fast (JIT-compiled) | ✅ 10x faster |
| **Cost** | $$$$ (per user) | Free (self-hosted) | ✅ Lower TCO |

**Aster Wins:** No vendor lock-in, full customization, performance, cost
**Low-Code Wins:** Faster prototyping for simple apps
**Target:** Aster for business-critical logic, low-code for CRUD UIs

---

## Conclusion & Next Steps

### Immediate Actions (Week 1)
1. **Assemble team:** Hire 2-3 senior engineers (compiler, runtime, frontend)
2. **Set up infrastructure:** GitHub org, CI/CD, Docker registry
3. **Kick off Phase 0:** Effect system enforcement, Truffle completion, docs

### 3-Month Milestones
- [ ] Phase 0 complete (foundation hardened)
- [ ] 1 pilot customer signed (FinTech or InsurTech)
- [ ] Phase 1 started (policy engine)

### 6-Month Milestones
- [ ] Phase 1 complete (policy engine shipped)
- [ ] 1M+ policy evaluations in production
- [ ] Phase 2 started (workflow orchestration)

### 12-Month Milestones
- [ ] Phase 2 complete (workflow engine shipped)
- [ ] 100K+ workflow executions in production
- [ ] Phase 3 started (compliance & AI)

### 24-Month Milestones
- [ ] Phase 4 complete (ecosystem & scaling)
- [ ] 10+ production deployments
- [ ] 50+ community contributors
- [ ] v1.0 release (production-ready)

---

**This roadmap is a living document.** Update quarterly based on customer feedback, technical discoveries, and market conditions. Prioritize ruthlessly: **ship early, iterate based on real-world usage, and focus on the five killer app domains.**


