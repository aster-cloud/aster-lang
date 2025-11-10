[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: inferCapabilityFromName()

> **inferCapabilityFromName**(`name`): `null` \| [`CapabilityKind`](../enumerations/CapabilityKind.md)

Defined in: [config/semantic.ts:166](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/config/semantic.ts#L166)

根据函数名前缀推断 capability。

## Parameters

### name

`string`

函数名或调用目标

## Returns

`null` \| [`CapabilityKind`](../enumerations/CapabilityKind.md)

推断的 CapabilityKind，如果无法推断则返回 null

## Example

```typescript
inferCapabilityFromName('Http.get')  // CapabilityKind.HTTP
inferCapabilityFromName('Db.query')  // CapabilityKind.SQL
inferCapabilityFromName('myFunc')    // null
```
