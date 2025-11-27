[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: parseEffect()

> **parseEffect**(`effect`): [`Effect`](../enumerations/Effect.md) \| `null`

Defined in: [config/semantic.ts:76](https://github.com/wontlost-ltd/aster-lang/blob/5ce39d5f2a99b2ded2bd237574687609f443e72d/src/config/semantic.ts#L76)

转换 effect 字符串为枚举。

## Parameters

### effect

`string`

CNL 中的 effect 字符串（小写）

## Returns

[`Effect`](../enumerations/Effect.md) \| `null`

对应的 Effect 枚举，如果非法则返回 null

## Example

```typescript
parseEffect('io')   // Effect.IO
parseEffect('cpuu') // null
```
