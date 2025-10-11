[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Function: canonicalize()

> **canonicalize**(`input`): `string`

Defined in: [canonicalizer.ts:80](https://github.com/wontlost-ltd/aster-lang/blob/424562fcc358f728754d52c653dd622346cf0157/src/canonicalizer.ts#L80)

规范化 CNL 源代码为标准格式。

这是 Aster 编译管道的第一步，将原始 CNL 文本转换为规范化的格式，
以便后续的词法分析和语法分析阶段处理。

**转换步骤**：
1. 规范化换行符为 `\n`
2. 将制表符转换为 2 个空格
3. 移除行注释（`//` 和 `#`）
4. 规范化引号（智能引号 → 直引号）
5. 强制语句以句号或冒号结尾
6. 去除冠词（a, an, the）
7. 规范化多词关键字大小写（如 "This module is" → "This module is"）

## Parameters

### input

`string`

原始 CNL 源代码字符串

## Returns

`string`

规范化后的 CNL 源代码

## Example

```typescript
import { canonicalize } from '@wontlost-ltd/aster-lang';

const raw = `
This Module Is app.
To greet, produce Text:
  Return "Hello"
`;

const canonical = canonicalize(raw);
// 输出：规范化后的代码，包含正确的句号和关键字大小写
```
