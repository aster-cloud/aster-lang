[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: parseEffect()

> **parseEffect**(`effect`): `null` \| [`Effect`](../enumerations/Effect.md)

Defined in: [config/semantic.ts:76](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/config/semantic.ts#L76)

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
