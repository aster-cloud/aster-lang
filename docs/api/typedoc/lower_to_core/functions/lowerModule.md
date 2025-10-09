[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Function: lowerModule()

> **lowerModule**(`ast`): [`Module`](../../types/namespaces/Core/interfaces/Module.md)

Defined in: [lower\_to\_core.ts:67](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/lower_to_core.ts#L67)

将 AST Module 降级为 Core IR Module。

这是 Aster 编译管道的第四步，将高级的抽象语法树转换为更简洁、更严格的 Core IR，
以便进行类型检查和后续的代码生成。

**降级转换**：
- 展开语法糖（`User?` → `Maybe of User`、`Result of A and B` → 标准 Result 类型）
- 规范化函数体为 Block 和 Statement 序列
- 将模式匹配转换为 Core IR 的 Case 结构
- 保留原始位置信息用于错误报告

## Parameters

### ast

[`Module`](../../types/interfaces/Module.md)

AST Module 节点（通过 parser.parse 生成）

## Returns

[`Module`](../../types/namespaces/Core/interfaces/Module.md)

Core IR Module 节点，包含所有声明的规范化表示

## Example

```typescript
import { canonicalize, lex, parse, lowerModule } from '@wontlost-ltd/aster-lang';

const src = `This module is app.
To greet, produce Text:
  Return "Hello".
`;

const ast = parse(lex(canonicalize(src)));
const core = lowerModule(ast);

console.log(core.kind);  // "Module"
console.log(core.name);  // "app"
// Core IR 是更简洁的表示，适合类型检查和代码生成
```
