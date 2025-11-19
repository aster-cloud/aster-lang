[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: inferCapabilityFromName()

> **inferCapabilityFromName**(`name`): [`CapabilityKind`](../enumerations/CapabilityKind.md) \| `null`

Defined in: [config/semantic.ts:166](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/config/semantic.ts#L166)

根据函数名前缀推断 capability。

## Parameters

### name

`string`

函数名或调用目标

## Returns

[`CapabilityKind`](../enumerations/CapabilityKind.md) \| `null`

推断的 CapabilityKind，如果无法推断则返回 null

## Example

```typescript
inferCapabilityFromName('Http.get')  // CapabilityKind.HTTP
inferCapabilityFromName('Db.query')  // CapabilityKind.SQL
inferCapabilityFromName('myFunc')    // null
```
