[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: isValidEffect()

> **isValidEffect**(`effect`): `effect is string`

Defined in: [config/semantic.ts:60](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/config/semantic.ts#L60)

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
