[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Function: getAllEffects()

> **getAllEffects**(): `string`[]

Defined in: [config/semantic.ts:90](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/config/semantic.ts#L90)

获取所有合法的 effect 字符串（用于错误提示）。

## Returns

`string`[]

合法的 effect 字符串数组

## Example

```typescript
getAllEffects() // ['io', 'cpu', 'pure']
```
