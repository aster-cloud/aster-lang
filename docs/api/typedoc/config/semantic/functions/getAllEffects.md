[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: getAllEffects()

> **getAllEffects**(): `string`[]

Defined in: [config/semantic.ts:90](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/config/semantic.ts#L90)

获取所有合法的 effect 字符串（用于错误提示）。

## Returns

`string`[]

合法的 effect 字符串数组

## Example

```typescript
getAllEffects() // ['io', 'cpu', 'pure']
```
