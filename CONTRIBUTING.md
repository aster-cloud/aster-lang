# Contributing to Aster

Thank you for your interest in contributing to Aster! This document provides guidelines and information for contributors.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Code Style](#code-style)
- [Testing](#testing)
- [Pull Request Process](#pull-request-process)
- [Issue Reporting](#issue-reporting)
- [Architecture Overview](#architecture-overview)

## Getting Started

Aster is a pragmatic, safe, fast programming language with human-readable CNL (Controlled Natural Language) syntax that compiles to JVM bytecode. The project consists of:

- **CNL Frontend**: Canonicalizer → Lexer → Parser → AST
- **Core IR**: Distinct intermediate representation with effect system
- **Type System**: Hindley-Milner-lite with non-null by default
- **Backends**: JVM bytecode and Truffle/GraalVM (planned)

## Development Setup

### Prerequisites

- Node.js 22+
- npm
- Git
- JDK 21+ (for JVM modules and examples)

### Setup

```bash
# Clone the repository
git clone https://github.com/wontlost-ltd/aster-lang.git
cd aster

# Install dependencies
npm install

# Build the project
npm run build

# Run tests
npm test

# Run the CLI
npm run build && node dist/scripts/cli.js cnl/examples/greet.aster
```

### Development Commands

```bash
# Development mode (watch for changes)
npm run dev

# Type checking
npm run typecheck

# Linting
npm run lint
npm run lint:fix

# Formatting
npm run format
npm run format:check

# Testing
npm run test:golden      # Golden snapshot tests
npm run test:property    # Property-based tests
npm run test:fuzz        # Fuzz tests
npm run bench           # Performance benchmarks

# Full CI pipeline (Node 22+, JDK 21+ for JVM checks)
npm run ci

# CI debug for LSP code actions (prints diagnostics and action titles)
CI_DEBUG=1 npm run ci
```

## Code Style

We use TypeScript with strict settings and enforce consistent code style:

### TypeScript Guidelines

- **Strict mode**: All TypeScript strict flags enabled
- **Explicit types**: Function return types must be explicit
- **No `any`**: Avoid `any` type; use proper types or `unknown`
- **Readonly**: Prefer `readonly` for arrays and objects that shouldn't be mutated
- **Null safety**: Use `| null` explicitly; avoid `undefined` in APIs

### Formatting

- **Prettier**: Automatic formatting with opinionated settings
- **Single quotes**: Use single quotes for strings
- **2 spaces**: Indentation (matches CNL syntax)
- **100 characters**: Line length limit
- **Trailing commas**: ES5 style

### Naming Conventions

- **Files**: kebab-case (`lexer.ts`, `core-ir.ts`)
- **Functions**: camelCase (`parseExpression`, `lowerModule`)
- **Types**: PascalCase (`TokenKind`, `Expression`)
- **Constants**: UPPER_SNAKE_CASE (`TOKEN_KIND`, `EFFECT_IO`)

## Testing

We maintain high test coverage with multiple testing strategies:

### Golden Tests

Snapshot tests that compare actual output with expected JSON:

```bash
npm run test:golden
```

Add new examples in `cnl/examples/` and run `npm run test:golden:update` to generate expected outputs.

### Property Tests

Property-based tests using fast-check:

```bash
npm run test:property
```

These test invariants like:
- Canonicalizer idempotency
- Lexer always produces EOF token
- Parser error handling with position info

### Fuzz Tests

Robustness tests with random inputs:

```bash
npm run test:fuzz
```

These ensure the lexer/parser don't crash on malformed input.

### Performance Tests

Benchmark critical paths:

```bash
npm run bench
```

We track performance regressions and maintain throughput targets.

## Pull Request Process

1. **Fork** the repository and create a feature branch
2. **Write tests** for new functionality
3. **Follow code style** (enforced by CI)
4. **Update documentation** if needed
5. **Run the full CI pipeline**: `npm run ci`
6. **Submit PR** with clear description

### PR Requirements

- [ ] All tests pass (`npm test`)
- [ ] Code is formatted (`npm run format:check`)
- [ ] No linting errors (`npm run lint`)
- [ ] TypeScript compiles (`npm run typecheck`)
- [ ] Performance benchmarks pass (`npm run bench`)
- [ ] Documentation updated if needed

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Added/updated tests
- [ ] All tests pass
- [ ] Performance impact assessed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
```

## Issue Reporting

### Bug Reports

Use the bug report template and include:

- **Aster version**: `npm list aster-lang`
- **Node.js version**: `node --version`
- **Operating system**: OS and version
- **Input code**: Minimal example that reproduces the issue
- **Expected behavior**: What should happen
- **Actual behavior**: What actually happens
- **Error output**: Full error message with stack trace

### Feature Requests

Use the feature request template and include:

- **Use case**: Why is this feature needed?
- **Proposed syntax**: How should it look in CNL?
- **Alternatives**: Other ways to achieve the same goal
- **Implementation**: Any thoughts on how to implement it

## Architecture Overview

### Pipeline

```
CNL Source → Canonicalizer → Lexer → Parser → CNL AST → Lowering → Core IR
```

### Key Modules

- **`src/canonicalizer.ts`**: Normalizes CNL text (whitespace, keywords, punctuation)
- **`src/lexer.ts`**: Tokenizes canonicalized text with INDENT/DEDENT handling
- **`src/parser.ts`**: Recursive descent parser producing CNL AST
- **`src/lower_to_core.ts`**: Lowers CNL AST to Core IR
- **`src/core_ir.ts`**: Core IR definitions with effect system
- **`src/diagnostics.ts`**: Structured error reporting with fix-its

### Design Principles

1. **Human-readable syntax**: CNL should read like natural language
2. **Type safety**: Non-null by default, explicit effects, exhaustive matching
3. **Performance**: Fast compilation, efficient runtime
4. **Tooling**: Great IDE support, helpful error messages
5. **Interoperability**: Seamless Java/JVM integration

### Adding New Features

1. **CNL syntax**: Update `src/tokens.ts` with new keywords
2. **Lexer**: Handle new token types in `src/lexer.ts`
3. **Parser**: Add parsing logic in `src/parser.ts`
4. **AST**: Define new node types in `src/types.ts` and `src/ast.ts`
5. **Core IR**: Add corresponding Core IR nodes if needed
6. **Lowering**: Update `src/lower_to_core.ts` to handle new nodes
7. **Tests**: Add golden tests, property tests, and examples

## Getting Help

- **Discussions**: Use GitHub Discussions for questions
- **Issues**: Report bugs and request features
- **Discord**: Join our community server (link in README)
- **Documentation**: Check the docs site at aster-lang.org

## License

By contributing to Aster, you agree that your contributions will be licensed under the MIT License.
