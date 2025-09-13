[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: If

Defined in: [types.ts:124](https://github.com/wontlost-ltd/aster-lang/blob/5be1f4ff28879f0dc1c32f364689abbfa13bc28f/src/types.ts#L124)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"If"`

Defined in: [types.ts:125](https://github.com/wontlost-ltd/aster-lang/blob/5be1f4ff28879f0dc1c32f364689abbfa13bc28f/src/types.ts#L125)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:126](https://github.com/wontlost-ltd/aster-lang/blob/5be1f4ff28879f0dc1c32f364689abbfa13bc28f/src/types.ts#L126)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types.ts:127](https://github.com/wontlost-ltd/aster-lang/blob/5be1f4ff28879f0dc1c32f364689abbfa13bc28f/src/types.ts#L127)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:128](https://github.com/wontlost-ltd/aster-lang/blob/5be1f4ff28879f0dc1c32f364689abbfa13bc28f/src/types.ts#L128)
