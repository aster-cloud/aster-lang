[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: getAllEffects()

> **getAllEffects**(): `string`[]

Defined in: [config/semantic.ts:90](https://github.com/wontlost-ltd/aster-lang/blob/0d39658db4faf20f48f10b0c8fb7fe31023ff8e1/src/config/semantic.ts#L90)

获取所有合法的 effect 字符串（用于错误提示）。

## Returns

`string`[]

合法的 effect 字符串数组

## Example

```typescript
getAllEffects() // ['io', 'cpu', 'pure']
```
