[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: If

Defined in: [types.ts:124](https://github.com/wontlost-ltd/aster-lang/blob/86413e9ea8f1137d326faff27f52785fcc703f39/src/types.ts#L124)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"If"`

Defined in: [types.ts:125](https://github.com/wontlost-ltd/aster-lang/blob/86413e9ea8f1137d326faff27f52785fcc703f39/src/types.ts#L125)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:126](https://github.com/wontlost-ltd/aster-lang/blob/86413e9ea8f1137d326faff27f52785fcc703f39/src/types.ts#L126)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types.ts:127](https://github.com/wontlost-ltd/aster-lang/blob/86413e9ea8f1137d326faff27f52785fcc703f39/src/types.ts#L127)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:128](https://github.com/wontlost-ltd/aster-lang/blob/86413e9ea8f1137d326faff27f52785fcc703f39/src/types.ts#L128)
