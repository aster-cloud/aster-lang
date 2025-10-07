[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Function: parse()

> **parse**(`tokens`): [`Module`](../../types/interfaces/Module.md)

Defined in: [parser.ts:74](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/parser.ts#L74)

将 Token 流解析为抽象语法树（AST）。

这是 Aster 编译管道的第三步，使用递归下降解析算法将 token 序列转换为结构化的 AST，
表示程序的语法结构。

**解析过程**：
1. 解析模块声明（`This module is ...`）
2. 解析顶层声明（导入、数据类型、枚举、函数）
3. 对每个函数解析参数、返回类型、效果标注和函数体
4. 递归解析语句和表达式

## Parameters

### tokens

readonly [`Token`](../../types/interfaces/Token.md)[]

Token 数组（应通过 lex 生成）

## Returns

[`Module`](../../types/interfaces/Module.md)

Module AST 节点，包含所有顶层声明

## Throws

当遇到语法错误时抛出（如缺少必需的 token、非法的语法结构等）

## Example

```typescript
import { canonicalize, lex, parse } from '@wontlost-ltd/aster-lang';

const src = `This module is app.
Define User with id: Text, name: Text.
To greet with user: User?, produce Text:
  Match user:
    When null, Return "Hi, guest".
    When User(id, name), Return "Welcome, {name}".
`;

const tokens = lex(canonicalize(src));
const ast = parse(tokens);

console.log(ast.kind);  // "Module"
console.log(ast.declarations.length);  // 2 (Data + Func)
```
