[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: getAllEffects()

> **getAllEffects**(): `string`[]

Defined in: [config/semantic.ts:90](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/config/semantic.ts#L90)

获取所有合法的 effect 字符串（用于错误提示）。

## Returns

`string`[]

合法的 effect 字符串数组

## Example

```typescript
getAllEffects() // ['io', 'cpu', 'pure']
```
