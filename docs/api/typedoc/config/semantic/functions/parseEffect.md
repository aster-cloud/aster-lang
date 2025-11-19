[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: parseEffect()

> **parseEffect**(`effect`): [`Effect`](../enumerations/Effect.md) \| `null`

Defined in: [config/semantic.ts:76](https://github.com/wontlost-ltd/aster-lang/blob/d5273adf2c0743cd88656e06c420ecec79c0e05e/src/config/semantic.ts#L76)

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
