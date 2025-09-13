# Quickstart

This guide shows how to build Aster, run the CLI, emit JVM artifacts, and try the Truffle interpreter.

## Prerequisites

- Node.js 22+, npm
- JDK 21+ (for JVM targets and examples); set `JAVA_HOME` accordingly

## Install & Build

```
npm ci
npm run build
```

Optionally scaffold a new project:

```
npm run new -- examples/hello-project
```

## Parse and Lower

```
# Parse CNL → AST (JSON)
node dist/scripts/cli.js cnl/examples/greet.cnl

# Lower to Core IR (JSON)
node dist/scripts/emit-core.js cnl/examples/greet.cnl
```

## Unified CLI

Use the single entry `aster` command (installed from `dist/`):

```
# Parse
node dist/scripts/aster.js parse cnl/examples/greet.cnl

# Core IR
node dist/scripts/aster.js core cnl/examples/greet.cnl

# Emit Java sources
node dist/scripts/aster.js jvm cnl/examples/greet.cnl --out build/jvm-src

# Emit classfiles (ASM)
node dist/scripts/aster.js class cnl/examples/login.cnl --out build/jvm-classes

# Jar emitted classes
node dist/scripts/aster.js jar

# Truffle: auto-lower .cnl and run Core IR, passing args to the function
node dist/scripts/aster.js truffle cnl/examples/if_param.cnl -- true
```

## Run Examples

```
# Text demo (mappings like concat/startsWith/indexOf)
npm run text:run

# List demo (length/get/isEmpty)
npm run list:run

# Map demo (get)
npm run map:run
```

## Validate ASM Output

Disassemble emitted classfiles to inspect bytecode:

```
npm run verify:asm
```

### Lambda Examples

Verify ASM output for Lambda fixtures:

```
# From Core JSON fixtures
npm run verify:asm:lambda

# From CNL examples (parse → lower → emit ASM)
npm run verify:asm:lambda:cnl
```

## Truffle Runner

Run Core IR on the Truffle interpreter via the CLI:

```
# From CNL (auto-lower)
node dist/scripts/aster.js truffle cnl/examples/if_param.cnl -- true

# From Core JSON
node dist/scripts/aster.js truffle build/if_param_core.json -- false
```

Notes:
- Values after `--` bind to function parameters (as strings for now).
- Current Truffle coverage is a subset: literals, names, let, if, return, and a few calls.
