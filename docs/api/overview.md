# API Overview

This section describes the main entry points of the compiler pipeline and their types.

```ts
import { canonicalize, lex, parse, lowerModule } from 'aster-lang';

const can = canonicalize(source);
const tokens = lex(can);
const ast = parse(tokens);
const core = lowerModule(ast);
```

- `canonicalize(source: string): string`
- `lex(input: string): Token[]`
- `parse(tokens: Token[]): Module`
- `lowerModule(ast: Module): Core.Module`

