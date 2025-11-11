[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: parseEffect()

> **parseEffect**(`effect`): `null` \| [`Effect`](../enumerations/Effect.md)

Defined in: [config/semantic.ts:76](https://github.com/wontlost-ltd/aster-lang/blob/9ecf3b8a788316f1e50f83cdb3f5ac701018ca90/src/config/semantic.ts#L76)

转换 effect 字符串为枚举。

## Parameters

### effect

`string`

CNL 中的 effect 字符串（小写）

## Returns

`null` \| [`Effect`](../enumerations/Effect.md)

对应的 Effect 枚举，如果非法则返回 null

## Example

```typescript
parseEffect('io')   // Effect.IO
parseEffect('cpuu') // null
```
