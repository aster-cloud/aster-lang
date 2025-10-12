[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Function: lex()

> **lex**(`input`): [`Token`](../../types/interfaces/Token.md)[]

Defined in: [lexer.ts:65](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/lexer.ts#L65)

对规范化的 CNL 源代码进行词法分析，生成 Token 流。

这是 Aster 编译管道的第二步，将规范化的文本字符串转换为结构化的 token 序列，
为后续的语法分析阶段提供输入。

**Token 类型**：
- 关键字：`To`, `Return`, `Match`, `When`, `Define`, `It performs` 等
- 标识符：变量名、函数名、类型名
- 字面量：整数、浮点数、布尔值、字符串、null
- 运算符：`+`, `-`, `*`, `/`, `=`, `==`, `<`, `>` 等
- 标点符号：`.`, `,`, `:`, `(`, `)`, `{`, `}` 等
- 特殊 token：`INDENT`, `DEDENT`, `NEWLINE`, `EOF`

## Parameters

### input

`string`

规范化后的 CNL 源代码（应先通过 canonicalizer.canonicalize 处理）

## Returns

[`Token`](../../types/interfaces/Token.md)[]

Token 数组，每个 token 包含类型、值和位置信息

## Throws

当遇到非法字符或缩进错误时抛出

## Example

```typescript
import { canonicalize, lex } from '@wontlost-ltd/aster-lang';

const src = `This module is app.
To greet, produce Text:
  Return "Hello".
`;

const canonical = canonicalize(src);
const tokens = lex(canonical);

// tokens 包含：MODULE_IS, IDENT("app"), DOT, TO, IDENT("greet"), ...
console.log(tokens.map(t => t.kind));
```
