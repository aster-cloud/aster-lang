[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: inferCapabilityFromName()

> **inferCapabilityFromName**(`name`): [`CapabilityKind`](../enumerations/CapabilityKind.md) \| `null`

Defined in: [config/semantic.ts:170](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/config/semantic.ts#L170)

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
