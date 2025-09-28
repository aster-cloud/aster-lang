[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: If

Defined in: [types.ts:124](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L124)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"If"`

Defined in: [types.ts:125](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L125)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:126](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L126)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types.ts:127](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L127)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:128](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L128)
