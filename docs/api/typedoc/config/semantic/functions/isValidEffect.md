[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: isValidEffect()

> **isValidEffect**(`effect`): `effect is string`

Defined in: [config/semantic.ts:60](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/config/semantic.ts#L60)

验证 effect 字符串是否合法。

## Parameters

### effect

`string`

CNL 中的 effect 字符串（小写）

## Returns

`effect is string`

如果是合法的 effect，返回 true

## Example

```typescript
isValidEffect('io')   // true
isValidEffect('cpuu') // false
```
