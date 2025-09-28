[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: If

Defined in: [types.ts:399](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L399)

## Extends

- [`CoreNode`](CoreNode.md)

## Properties

### kind

> `readonly` **kind**: `"If"`

Defined in: [types.ts:400](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L400)

#### Overrides

[`CoreNode`](CoreNode.md).[`kind`](CoreNode.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:401](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L401)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types.ts:402](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L402)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:403](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L403)
