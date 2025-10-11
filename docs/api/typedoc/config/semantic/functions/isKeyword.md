[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: isKeyword()

> **isKeyword**(`word`): `boolean`

Defined in: [config/semantic.ts:262](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/config/semantic.ts#L262)

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
