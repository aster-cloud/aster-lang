[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: getAllEffects()

> **getAllEffects**(): `string`[]

Defined in: [config/semantic.ts:90](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/config/semantic.ts#L90)

获取所有合法的 effect 字符串（用于错误提示）。

## Returns

`string`[]

合法的 effect 字符串数组

## Example

```typescript
getAllEffects() // ['io', 'cpu', 'pure']
```
