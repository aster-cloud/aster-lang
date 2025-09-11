# Getting Started

Welcome to Aster! This guide will help you try Aster locally.

## Prerequisites
- Node.js 22+

## Install and build

```bash
npm install
npm run build
```

## Parse a program

```bash
node dist/scripts/cli.js cnl/examples/greet.cnl
```

## Emit Core IR

```bash
node dist/scripts/emit-core.js cnl/examples/greet.cnl
```

## Run tests

```bash
npm test
```

## Next steps
- Read the [Language Overview](/guide/language-overview)
- Explore the [Syntax Reference](/reference/syntax)
- Check out the [API](/api/overview)
