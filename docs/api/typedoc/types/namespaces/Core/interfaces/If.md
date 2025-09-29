[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: If

Defined in: [types.ts:417](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L417)

## Extends

- [`CoreNode`](CoreNode.md)

## Properties

### kind

> `readonly` **kind**: `"If"`

Defined in: [types.ts:418](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L418)

#### Overrides

[`CoreNode`](CoreNode.md).[`kind`](CoreNode.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:419](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L419)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types.ts:420](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L420)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:421](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L421)
