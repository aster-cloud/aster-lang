[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: isKeyword()

> **isKeyword**(`word`): `boolean`

Defined in: [config/semantic.ts:262](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/config/semantic.ts#L262)

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
