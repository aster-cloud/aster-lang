[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: inferCapabilityFromName()

> **inferCapabilityFromName**(`name`): [`CapabilityKind`](../enumerations/CapabilityKind.md) \| `null`

Defined in: [config/semantic.ts:166](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/config/semantic.ts#L166)

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
