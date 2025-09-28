# OPTIMISE.md
_PhD-level optimisation plan for **aster-lang**, updated from concrete evidence in `javap -v` dumps and Gradle logs you shared._

This plan fixes **correctness first** (bytecode descriptors, field access, package mapping), then **native-image friendliness**, **performance**, and **DX**. Each item lists **Symptoms → Root cause → Actions → Acceptance** you can execute _from scratch_.

---

## 0) Critical bytecode correctness

- [x] Fully‑qualified JVM descriptors for user types (no `LAction;`, `LPolicyContext;`)
- [x] Instance field access via `getfield` (no synthetic statics)
- [x] Function value fields typed as FnN (not applicable; direct JDK calls used)
- [x] Package mapping consistency + `package-map.json`

### 0.1 **Unqualified descriptors** in method signatures
**Symptoms**  
`demo.simple_policy.canAccess_fn` and `demo.test_policy.canAccess_fn` show:
```
descriptor: (Ljava/lang/String;LAction;Ljava/lang/String;Ljava/lang/String;)Z
```
…and `evaluatePolicy_fn` shows:
```
descriptor: (LPolicyContext;)Z
```
Both use **unqualified** `LAction;`, `LPolicyContext;`.

**Root cause**  
Descriptor encoder uses **simple names** for user types instead of **internal names** (`pkg/Name`).

**Actions**
1. In the emitter’s “type → JVM descriptor” function, always build the **internal name**:
   - `internal = package.replace('.', '/') + '/' + simpleName`
   - `desc = 'L' + internal + ';'`
2. Ensure **method params and returns** use this logic.
3. Add a golden test: a function `boolean f(UserId, Action)` must show
   ```
   descriptor: (Ldemo/policy/UserId;Ldemo/policy/Action;)Z
   ```

**Acceptance**
- `javap -v` of all exported functions contains **fully-qualified** descriptors; `examples:login-jvm` compiles.

- [x] Completed — descriptors now use fully-qualified internal names; a blocking `javap:verify` CI check guards regressions.

---

### 0.2 **Incorrect field-access lowering** for record fields
**Symptoms**  
`evaluatePolicy_fn` emits reads like:
```
getstatic demo/simple_policy/context.userRole:Ldemo/simple_policy/context;
```
It never uses the function parameter; fields appear as **static members** on a synthetic `context` class.

**Root cause**  
Lowering treats record-field access as global/static instead of reading from the **instance**.

**Actions**
1. For `PolicyContext` parameter in local slot `0`, generate:
   ```
   aload_0
   getfield demo/simple_policy/PolicyContext.userRole:Ljava/lang/String;
   ```
   Similarly for `userId`, `resourceOwner`, etc.
2. Remove any synthetic `context` holder class; delete `getstatic`-style access for record fields.
3. Add tests that call `evaluatePolicy(PolicyContext)` and assert `javap` shows `getfield` on the instance.

**Acceptance**
- `evaluatePolicy_fn` uses `aload_0; getfield ...PolicyContext.field:Type;` (no `getstatic context.*`).

- [x] Completed — emitter uses instance field access for records; no synthetic statics.

---

### 0.3 **Function value fields have the wrong type**
**Symptoms**  
`canAccess_fn`/`evaluatePolicy_fn` use:
```
getstatic demo/simple_policy/Text.equals:Ldemo/simple_policy/Text;
invokeinterface aster/runtime/Fn2.apply:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
```
The field `Text.equals` is typed as **Text**, but used as **Fn2**.

**Root cause**  
When emitting fields that hold function values (e.g., `equals`), the **field descriptor** is incorrectly set to the enclosing data type instead of the function interface type.

**Actions**
1. When a module exports a function value, emit:
   ```
   .field public static final equals Laster/runtime/Fn2;
   ```
   not `Ldemo/.../Text;`.
2. At use sites, either:
   - call the underlying JDK method directly (preferred for primitives/Strings/enums), or
   - load the `Fn2` and `invokeinterface Fn2.apply`.
3. Add verifier tests to catch mismatched field descriptors.

**Acceptance**
- `javap` shows `Field demo/.../Text.equals:Laster/runtime/Fn2;` (or no field when directly inlining).
- `-Xverify:all` clean.

- [x] Not applicable for current design — functions are emitted as static methods/closures, not as module fields. Direct JDK inlining is used where appropriate (e.g., `Text.equals`).

---

### 0.4 **Package mapping mismatch** between emitted classes and examples
**Symptoms**  
Examples use `demo.policy.*`, while emitter produces `demo.simple_policy.*`, `demo.test_policy.*`, `demo.policy_demo.*`.

**Root cause**  
Non-canonical mapping from CNL module names → Java packages.

**Actions**
1. Define **one rule** (document it):
   - Example: `module "demo.policy"` → Java package `demo.policy` (internal `demo/policy`).
2. Emit a build artifact `build/aster-out/package-map.json`:
   ```json
   { "modules": [{ "cnl": "demo.policy", "jvm": "demo.policy" }] }
   ```
3. Either update the emitter to match `demo.policy.*` or patch examples to the actual package.
4. CI step: assert examples compile against the emitted package map.

**Acceptance**
- `examples:login-jvm` compiles; no “cannot access Action/Resource/PolicyContext”.

- [x] Completed — emitter writes `build/aster-out/package-map.json`; descriptors honor module packages; default package fallback added.

---

## 1) Native-image friendliness (GraalVM)

- [x] Emit debug attributes (`SourceFile`, `LineNumberTable`, `LocalVariableTable`)
- [x] StackMap hygiene (COMPUTE_FRAMES, unified joins)
- [x] Reflection-free policy docs + native lane sample (docs/reference/native.md; CI lenient lane)

### 1.1 Keep code **reflection-free** and avoid proxies/indy
**Actions**
- Generate **final classes + static methods**; avoid reflection and dynamic proxies.
- If reflection is ever introduced, auto-emit `META-INF/native-image/.../reflect-config.json`.
- CI lane runs `nativeCompile` on a sample app with `--no-fallback --strict-image-heap`.

### 1.2 Emit full **debug attributes**
**Symptoms**  
Some classes only have `Code` attributes.

**Actions**
- Emit `SourceFile`, `LineNumberTable`, `LocalVariableTable` for every method.
- CI gate: fail if any public method lacks line numbers.

- [x] Completed — classes now include `SourceFile`, method-level `LineNumberTable` and `LocalVariableTable`; nested bodies carry statement-level line markers.

### 1.3 **StackMapTable** hygiene & unreachable code
**Symptoms**  
Many repeated **full frames** and `nop; nop; athrow` placeholders.

**Actions**
- Only emit frames at **basic-block joins** and exception handlers.
- Remove unreachable blocks; do not synthesize `athrow` unless inside a real handler.
- If you are using ASM, prefer `COMPUTE_FRAMES` and provide correct maxs; otherwise keep your own simple frame builder.

**Acceptance**
- `-Xverify:all` passes; StackMapTable shrinks; no stray `athrow` pads.

- [x] Completed — emitter relies on `COMPUTE_FRAMES`, unifies try/catch returns via join labels, and prunes extraneous labels in match where safe.

---

## 2) Emitter quality & micro-optimisations

- [x] Direct JDK calls on hot paths
- [x] Constant pool determinism/dedup (gate in CI done; dedup TBD)
- [x] Pattern matching lowering for sparse ints (`lookupswitch`) when applicable

### 2.1 Prefer **direct JDK calls** over function indirection
**Observations**  
Text ops already inline (`String.concat`, `length`, `indexOf`, `startsWith`), but policy code routes equality through `Fn2`.

**Actions**
- Lower equality as:
  - `String`: `invokevirtual java/lang/String.equals`
  - `enum`: `if_acmpeq` (load enum constants then compare)
- Reserve `Fn1/Fn2` only for genuine higher-order values.

**Acceptance**
- `canAccess_fn` shows `invokevirtual String.equals` or `if_acmpeq` (no `Fn2.apply` on the hot path).

- [x] Completed — direct JDK paths implemented for text ops (concat/contains/indexOf/startsWith/equals/etc.), arithmetic/comparisons, list/map helpers; enums compared by identity.

### 2.2 Constant pool determinism & dedup
**Actions**
- Intern CP entries; stable insertion order.
- CI determinism check: rebuild twice, classfile bytes must match (ignoring timestamped attributes).

### 2.3 Pattern matching lowering
**Actions**
- Use `tableswitch` for dense enums/ints, `lookupswitch` for sparse.
- Share join blocks and re-use local slots to keep StackMapTable compact.

---

## 3) Build & scripts hygiene (Node/TS)

- [x] Precompile scripts; remove ts-node loaders from tests
- [x] Remove all remaining loader warnings (if any stray usages remain)

**Symptoms**  
`node --loader ts-node/esm …` + `fs.Stats constructor is deprecated` warnings.

**Actions**
1. **Precompile** `scripts/*.ts` to `dist/scripts/*.js`; run with plain Node (no loader).
2. Replace any deprecated `new fs.Stats()` usage with `fs.statSync` return values.
3. Ensure CLI bins preserve shebang and ESM import suffixes (`.js` in built files).

**Acceptance**
- `npm run jar:jvm` and `verify:truffle:smoke` produce **no warnings**.

---

## 4) Interop layer hardening

- [x] Overload resolution policy (arity → exact → widening → boxing → varargs) (Moved to TODO.md)
- [x] Nullability defaults and overrides (Moved to TODO.md)
- [x] Lightweight classpath scanning + cache (Moved to TODO.md)

### 4.1 Overload resolution & nullability
**Actions**
- Deterministic rank: **arity → exact → primitive-widening → boxing → varargs**; ambiguity = compile error with cast hint.
- Default external returns to `T?`; allow per-symbol overrides via annotations.

### 4.2 Classpath scanning with zero heavy deps
**Actions**
- Parse JAR central directories; read descriptors & `Signature` attrs only.
- Cache scan results to `build/.asteri` for incremental builds.

---

## 5) Effects & capabilities (policy-as-types)

- [x] Capability manifest + compile-time checks (Moved to TODO.md)
- [x] LSP code actions for effects/capabilities (Moved to TODO.md)

**Actions**
- Capability manifest (YAML/JSON) describes allowed hosts/DBs/paths.
- Compile-time check: each `net|db|fs|time|cpu|rand` usage must map to a declared capability.
- LSP code actions: “Insert @io/@net”, “Add capability ‘crm’”.

---

## 6) Debuggability & provenance

- [x] Attach source spans + @AsterOrigin (Moved to TODO.md)
- [x] Structured logs include spans (Moved to TODO.md)

**Actions**
- Attach `(file,start,end)` to every IR node; emit runtime-retained `@AsterOrigin`.
- Logs include spans so traces can “hover back” to source.

---

## 7) LSP & formatter (DX)

- [x] Lossless CST + idempotent formatter fuzzing (Moved to TODO.md)
- [x] LSP feature/perf targets (Moved to TODO.md)

**Actions**
- Lossless CST → idempotent formatter (fuzz: `format(parse(format(src))) === format(src)`).
- LSP: hover/types/effects, go-to-def, find-refs, rename, semantic tokens, quick-fixes; debounce + incremental typecheck; target p50 < **30ms** on 100-file workspace.

---

## 8) Tests & CI gates (add now)

- [x] Golden pipeline (AST → Core → bytecode)
- [x] Descriptor check via `javap:verify`
- [x] Class verification under `-Xverify:all` (blocking)
- [x] Determinism (blocking)
- [x] Verifier fuzz (non‑blocking) — expanded coverage and integrated as CI lane
- [x] Native lane (non‑blocking) — lenient CI step added (toolchain dependent)

- **Golden**: AST → IR → bytecode → `javap -v` snapshots for the policy and text examples.
- **Descriptor**: assert no unqualified `LAction;` / `LPolicyContext;` remain.
- **Verifier fuzz**: random CFGs under `-Xverify:all`.
- **Determinism**: identical classfiles across two builds.
- **Native lane**: sample app `nativeCompile` green with no reflect configs.

---

## 9) Example fixes (make tutorials green)

- [x] CI assertion examples use package map (Moved to TODO.md)

- Either emit into `demo.policy.*` or update `examples/login-jvm` to import `demo.simple_policy.*` consistently.
- Add a post-emit step to write a short **package map** and assert examples use it.

---

### Quick acceptance checklist

- [x] All method descriptors fully‑qualified (no `LAction;`, `LPolicyContext;`).
- [x] Record fields read via `getfield` on the instance (no `getstatic context.*`).
- [x] Function value fields typed as `Laster/runtime/Fn1|Fn2;` or inlined to direct JDK calls (inlined where applicable).
- [x] No `ts-node` loader or `fs.Stats` warnings in scripts.
- [x] `-Xverify:all` clean; StackMapTable compact, no stray `athrow` (class verification lane).
- [x] `examples:login-jvm` compiles; `javap verification completed` remains green.

---
