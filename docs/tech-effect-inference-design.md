# Effect Inference System: Technical Design Document

## Architecture Overview

The effect inference system in Aster uses constraint-based analysis to determine the effects of functions, with support for polymorphic effect variables.

### Key Components

```
┌─────────────────────────────────────────────────────┐
│                  Parser Layer                        │
│  (Parse effect annotations & effect variables)       │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│                Type System Layer                     │
│  (EffectVar type, type unification)                  │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│              Effect Inference Layer                  │
│  ┌─────────────────────────────────────────────┐    │
│  │  EffectCollector (visitor pattern)          │    │
│  │  - Visit function bodies                    │    │
│  │  - Collect local effects                    │    │
│  │  - Build call constraints                   │    │
│  │  - Handle Lambda expressions                │    │
│  └─────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────┐    │
│  │  EffectPropagator                           │    │
│  │  - Build SCC graph (Tarjan's algorithm)     │    │
│  │  - Topological sort                         │    │
│  │  - Iterative fixpoint computation          │    │
│  │  - Effect binding propagation               │    │
│  └─────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────┐    │
│  │  DiagnosticBuilder                          │    │
│  │  - Detect missing effects                   │    │
│  │  - Detect redundant effects                 │    │
│  │  - Detect unresolved effect variables       │    │
│  └─────────────────────────────────────────────┘    │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│               Module Cache Layer                     │
│  (Cross-module effect signatures & invalidation)     │
└─────────────────────────────────────────────────────┘
```

## Data Structures

### EffectRef

```typescript
type EffectAtom = Effect | 'Workflow';
type EffectRef = EffectAtom | { kind: 'EffectVar'; name: string };
```

An `EffectRef` can be either:
- A concrete effect atom (PURE, CPU, IO, Workflow)
- An effect variable (e.g., `{kind: 'EffectVar', name: 'E'}`)

### EffectBinding

```typescript
type EffectBinding = {
  value: EffectAtom;     // Current bound value
  resolved: boolean;      // Whether bound to non-PURE
};
```

Tracks the binding state of an effect variable.

### EffectBindingTable

```typescript
type EffectBindingTable = Map<string, Map<string, EffectBinding>>;
//                             ↑funcName  ↑varName
```

Maps functions to their effect variable bindings.

### EffectConstraint

```typescript
interface EffectConstraint {
  caller: string;     // Calling function name
  callee: string;     // Called function name
  location?: Origin;  // Source location for diagnostics
}
```

Represents a caller → callee dependency in the call graph.

## Algorithm

### Phase 1: Effect Collection

For each function:
1. Visit function body using EffectCollector visitor
2. Collect local effects from:
   - Builtin calls (Http.get → IO, Analytics.compute → CPU)
   - Workflow statements (workflow → IO)
   - Lambda body effects (recursively)
3. Record call constraints (caller → callee edges)

**Pseudocode**:

```
function collectEffects(func):
  localEffects = Set()
  constraints = []
  bindings = initBindings(func.effectParams)

  visitor = new EffectCollector(localEffects, constraints)
  visitor.visitBlock(func.body)

  return {constraints, localEffects, bindings}
```

### Phase 2: Constraint Graph Construction

Build a directed graph where:
- **Nodes**: Function names
- **Edges**: Call relationships (constraints)

**Pseudocode**:

```
function buildGraph(constraints, effectMap):
  adjacency = Map<string, Set<string>>()

  for constraint in constraints:
    if effectMap.has(constraint.callee):
      adjacency.get(constraint.caller).add(constraint.callee)

  return adjacency
```

### Phase 3: SCC Detection (Tarjan's Algorithm)

Find strongly connected components (SCCs) to handle mutual recursion:

```
function runTarjan(nodes, adjacency):
  index = 0
  stack = []
  visited = Map()
  components = []

  for node in nodes:
    if not visited.has(node):
      strongConnect(node)

  return components
```

### Phase 4: Effect Propagation

Propagate effects through the call graph:

1. **Seed bindings**: Initialize effect variables with local effects
2. **Topological order**: Process SCCs in reverse topological order
3. **SCC fixpoint**: For each SCC, iterate until no changes
4. **Cross-SCC merge**: Propagate effects between components

**Pseudocode**:

```
function propagateEffects(constraints, effectMap, bindings):
  seedBindings WithEffects(effectMap, bindings)

  {components, componentByNode} = runTarjan(nodes, adjacency)
  order = topologicalSort(components)

  for componentIndex in order:
    members = components[componentIndex]

    // Intra-SCC fixpoint
    repeat until no changes:
      for node in members:
        for neighbor in members:
          mergeEffects(node, neighbor, effectMap, bindings)

    // Cross-SCC propagation
    for node in members:
      for neighbor not in same SCC:
        mergeEffects(node, neighbor, effectMap, bindings)
```

### Phase 5: Diagnostic Generation

Check for effect mismatches:

```
function buildDiagnostics(funcs, effectMap, bindings):
  diagnostics = []

  for func in funcs:
    declared = func.effects
    inferred = resolveEffects(effectMap.get(func.name), bindings)

    // Check missing effects
    for effect in (inferred - declared):
      diagnostics.add(EFF_INFER_MISSING(func, effect))

    // Check redundant effects
    for effect in (declared - inferred):
      diagnostics.add(EFF_INFER_REDUNDANT(func, effect))

    // Check unresolved effect variables
    for varName in func.effectParams:
      binding = bindings.get(func.name).get(varName)
      if not binding.resolved:
        diagnostics.add(EFFECT_VAR_UNRESOLVED(func, varName))

  return diagnostics
```

## Lambda Effect Handling

### Design Decision

**Strategy**: Conservative propagation - Lambda body effects are collected immediately upon Lambda definition and contribute to the enclosing function's effects.

**Rationale**:
- Simpler implementation without data-flow analysis
- Ensures no effects are missed (soundness over precision)
- Practical for most use cases

### Implementation

```typescript
// In EffectCollector.visitExpression
if (e.kind === 'Lambda') {
  const lambda = e as Core.Lambda;
  this.visitBlock(lambda.body, context);
  return; // Lambda body processed, skip default recursion
}
```

### Example

```aster
fn process(): Text with IO.
  Let fetcher be function with url: Text, produce Text:
    Return Http.get(url).  // IO effect collected here
  Return fetcher("/data").
```

Effect flow:
1. EffectCollector visits `process` body
2. Encounters Lambda expression
3. Visits Lambda body, finds `Http.get` → adds IO to `process` effects
4. Result: `process` correctly inferred to require IO

## Cross-Module Effect Propagation

### Module Cache Design

```typescript
interface EffectSignature {
  readonly module: string;
  readonly function: string;
  readonly qualifiedName: string;
  readonly declared: ReadonlySet<Effect>;
  readonly inferred: ReadonlySet<Effect>;
  readonly required: ReadonlySet<Effect>;  // declared ∪ inferred
}
```

### Caching Strategy

1. **Write**: After inferring effects, cache signatures by module
2. **Read**: When analyzing module B that imports module A, load A's cached signatures
3. **Invalidation**: When A changes, recursively invalidate A and all dependents

**Pseudocode**:

```
function cacheModuleEffects(moduleName, signatures, imports):
  effectSignaturesByModule.set(moduleName, signatures)
  updateDependencies(moduleName, imports)

function loadImportedEffects(context):
  for moduleName in context.imports:
    cached = getModuleEffectSignatures(moduleName)
    if cached:
      context.importedEffects.merge(cached)

function invalidateModuleEffects(moduleName):
  visited = Set()
  invalidateRecursive(moduleName, visited)

function invalidateRecursive(moduleName, visited):
  if visited.has(moduleName): return
  visited.add(moduleName)

  effectSignaturesByModule.delete(moduleName)

  for dependent in dependentsByModule.get(moduleName):
    invalidateRecursive(dependent, visited)
```

## Effect Lattice

The effect hierarchy forms a lattice:

```
        Workflow
           |
          IO
           |
          CPU
           |
         PURE
```

**Comparison function**:

```typescript
function effectRank(atom: EffectAtom): number {
  switch (atom) {
    case Effect.PURE: return 0;
    case Effect.CPU: return 1;
    case Effect.IO: return 2;
    default: return 3;  // Workflow
  }
}

function strongerEffect(a: EffectAtom, b: EffectAtom): EffectAtom {
  return effectRank(a) >= effectRank(b) ? a : b;
}
```

## Performance Characteristics

### Time Complexity

- **Effect Collection**: O(n) where n = number of AST nodes
- **SCC Detection**: O(V + E) where V = functions, E = calls (Tarjan's algorithm)
- **Effect Propagation**: O(V + E) × k where k = average SCC iterations (typically small)
- **Overall**: O(V + E) in practice

### Space Complexity

- **Effect Map**: O(V)
- **Binding Table**: O(V × P) where P = effect params per function
- **Adjacency List**: O(E)
- **Overall**: O(V + E)

### Optimization Opportunities

1. **Incremental Analysis**: Only re-analyze changed modules
2. **Effect Caching**: Cache module-level effect signatures
3. **Lazy Propagation**: Skip SCCs with no unresolved variables

## Error Codes

| Code | Name | Description |
|------|------|-------------|
| E101 | EFF_INFER_MISSING_IO | Function performs IO but doesn't declare `It performs io` |
| E102 | EFF_INFER_MISSING_CPU | Function performs CPU but doesn't declare `It performs cpu` |
| E103 | EFF_INFER_REDUNDANT_IO | Function declares IO but doesn't perform any |
| E104 | EFF_INFER_REDUNDANT_CPU | Function declares CPU but doesn't perform any |
| E210 | EFFECT_VAR_UNDECLARED | Effect variable used but not declared in signature |
| E211 | EFFECT_VAR_UNRESOLVED | Effect variable cannot be inferred to concrete effect |

## Testing Strategy

### Unit Tests

- Effect collection (builtin calls, workflow, Lambda)
- Constraint graph construction
- SCC detection (mutual recursion, self-recursion)
- Effect propagation (direct, transitive, diamond)
- Effect variable binding
- Cross-module propagation
- Diagnostic generation

### Integration Tests

- Complete type checking pipeline
- LSP diagnostics and quick fixes
- Multi-module projects

### Golden Tests

- Canonical effect polymorphism examples
- Regression test suite

## Future Enhancements

### 1. Precise Lambda Effect Tracking

Current limitation: Lambda effects propagate immediately to enclosing function.

**Improvement**: Track Lambda-to-variable bindings and analyze call sites:

```
Let f = lambda with x: ...  // Track: f → lambda
Call g(f)                    // Analyze: g's signature to determine f's effect usage
```

### 2. Effect Subtyping Constraints

Support constraints like `E <: IO` (E must be at most IO):

```aster
fn limited of E <: IO(x: Int with E): Int with E.
  // E can only be PURE, CPU, or IO (not Workflow)
```

### 3. Effect Inference Trace

Provide detailed trace showing how effects were inferred:

```
Function 'outer' inferred IO because:
  → calls 'inner' (line 10)
    → calls 'Http.get' (line 5)
      → builtin IO effect
```

### 4. Effect Capabilities Integration

Unify effect variables with capability system for fine-grained control.

## References

1. **Type Systems**: Pierce, Benjamin C. "Types and Programming Languages"
2. **Effect Systems**: Lucassen, J.M. and Gifford, D.K. "Polymorphic Effect Systems"
3. **Tarjan's SCC**: Tarjan, Robert. "Depth-First Search and Linear Graph Algorithms"
4. **Constraint Solving**: Nielson, Flemming. "Principles of Program Analysis"

## Appendix: Code References

- **Effect Inference**: `src/effect_inference.ts`
- **Type System**: `src/typecheck/type_system.ts`
- **Module Cache**: `src/lsp/module_cache.ts`
- **Effect Signature**: `src/effect_signature.ts`
- **Error Codes**: `src/error_codes.ts`
- **Tests**: `test/unit/effect/effect-inference.test.ts`
