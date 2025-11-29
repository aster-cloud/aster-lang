[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: isKeyword()

> **isKeyword**(`word`): `boolean`

Defined in: [config/semantic.ts:275](https://github.com/wontlost-ltd/aster-lang/blob/e4b7a41c141fab64c03c5017e7bd6371419cde5c/src/config/semantic.ts#L275)

验证字符串是否为关键字。

## Parameters

### word

`string`

要检查的字符串（应为小写）

## Returns

`boolean`

如果是关键字，返回 true

## Example

```typescript
isKeyword('return')     // true
isKeyword('myVariable') // false
```
