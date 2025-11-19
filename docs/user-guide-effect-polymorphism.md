# Effect Polymorphism User Guide

## Overview

Effect polymorphism allows functions to be generic over effects, enabling more flexible and reusable code. This feature is essential for building polymorphic higher-order functions like `map`, `filter`, and `fold`.

## Basic Syntax

### Effect Variables

Effect variables are declared in the function signature using the `of` clause:

```aster
fn identity of T, E(x: T with E): T with E.
  Return x.
```

Here:
- `T` is a type variable (generic type parameter)
- `E` is an effect variable (generic effect parameter)
- `with E` declares that the function performs effect `E`

### Calling Polymorphic Functions

When calling a polymorphic function, the effect variable is instantiated based on usage:

```aster
fn caller(): Unit with IO.
  Call identity(42).  // E instantiated to PURE
```

## Common Use Cases

### 1. Higher-Order Functions

```aster
fn map of T, R, E(
  list: List<T>,
  f: (T with E) -> R
): List<R> with E.
  // Map implementation
```

### 2. Effect Propagation

Effect variables automatically propagate effects from callbacks:

```aster
fn apply of T, R, E(
  x: T,
  f: (T with E) -> R
): R with E.
  Return f(x).
```

### 3. Lambda with Effects

Lambdas can have effects that propagate to the enclosing function:

```aster
fn process_with_lambda, produce Text. It performs io:
  Let fetcher be function with endpoint: Text, produce Text:
    Return Http.get(endpoint).  // IO effect propagates
  Return fetcher("/api/data").
```

## Effect Lattice

Effects form a lattice with the following hierarchy:

```
PURE < CPU < IO < Workflow
```

- **PURE**: No side effects
- **CPU**: Computation only (no I/O)
- **IO**: I/O operations (network, file system, etc.)
- **Workflow**: Orchestrated multi-step workflows

## Best Practices

### 1. Be Explicit About Effects

Always declare effects explicitly to improve code readability:

```aster
// Good
fn process of E(x: Int with E): Int with E.
  Return x * 2.

// Avoid (implicit effect)
fn process(x: Int): Int.
  Return x * 2.
```

### 2. Use Effect Variables for Generic Code

When writing generic utilities, use effect variables:

```aster
// Generic map function
fn map of T, R, E(
  items: List<T>,
  transform: (T with E) -> R
): List<R> with E.
  // Implementation
```

### 3. Understand Effect Propagation

Effects propagate upwards through the call chain:

```aster
fn inner(): Unit with IO.
  Call Http.get("/data").

fn outer() of E: Unit with E.
  Call inner().  // E instantiated to IO
```

## Error Messages

### E210: Effect Variable Undeclared

```
Effect variable 'E' undeclared
```

**Fix**: Add the effect variable to the function signature:

```aster
// Before
fn foo(x: Int with E): Int with E.

// After
fn foo of E(x: Int with E): Int with E.
```

### E211: Effect Variable Unresolved

```
Effect variable {vars} cannot be inferred
```

**Fix**: Provide a concrete effect or add constraints:

```aster
// Option 1: Specify concrete effect
fn foo(): Unit with IO.
  // Use IO operations

// Option 2: Add effect parameter
fn foo of E(): Unit with E.
  // Parameterize over effect
```

## Advanced Topics

### Effect Constraints

(To be documented - future feature)

### Effect Inference Algorithm

(See technical documentation: docs/effect-inference-design.md)

## Examples

### Example 1: Polymorphic Identity

```aster
fn identity of T, E(x: T with E): T with E.
  Return x.

fn test_pure(): Int.
  Return identity(42).  // E = PURE

fn test_io(): Text with IO.
  Return identity(Http.get("/data")).  // E = IO
```

### Example 2: Polymorphic Map

```aster
fn map of T, R, E(
  items: List<T>,
  f: (T with E) -> R
): List<R> with E.
  // Implementation using effect variable E
```

### Example 3: Nested Lambdas with Effects

```aster
fn complex_processing(): Text with IO.
  Let outer be function with url: Text, produce Text:
    Let inner be function with endpoint: Text, produce Text:
      Return Http.get(endpoint).
    Return inner(url).
  Return outer("/api/data").
```

## Troubleshooting

### My effect variable is not being inferred

**Reason**: The effect variable must be used in a way that allows inference.

**Solution**: Ensure the effect variable appears in parameter or return types with `with E` annotation.

### I get "effect variable unresolved" error

**Reason**: The compiler cannot determine a concrete effect for the variable.

**Solution**: Either specify a concrete effect or ensure the function is called with concrete effect arguments.

## See Also

- [Technical Design Document](./effect-inference-design.md)
- [Effect System Overview](./effects.md)
- [Type System Documentation](./type-system.md)
