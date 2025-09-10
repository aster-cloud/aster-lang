[**aster-lang**](../../../../README.md)

***

# Interface: If

Defined in: [types.ts:365](https://github.com/wontlost-ltd/aster-lang/blob/b53f24492721cfed9e6a591422966205af1dad8e/src/types.ts#L365)

## Extends

- [`CoreNode`](CoreNode.md)

## Properties

### kind

> `readonly` **kind**: `"If"`

Defined in: [types.ts:366](https://github.com/wontlost-ltd/aster-lang/blob/b53f24492721cfed9e6a591422966205af1dad8e/src/types.ts#L366)

#### Overrides

[`CoreNode`](CoreNode.md).[`kind`](CoreNode.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:367](https://github.com/wontlost-ltd/aster-lang/blob/b53f24492721cfed9e6a591422966205af1dad8e/src/types.ts#L367)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types.ts:368](https://github.com/wontlost-ltd/aster-lang/blob/b53f24492721cfed9e6a591422966205af1dad8e/src/types.ts#L368)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:369](https://github.com/wontlost-ltd/aster-lang/blob/b53f24492721cfed9e6a591422966205af1dad8e/src/types.ts#L369)
