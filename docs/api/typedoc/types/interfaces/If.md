[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: If

Defined in: [types.ts:126](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L126)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"If"`

Defined in: [types.ts:127](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L127)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:128](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L128)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types.ts:129](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L129)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:130](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L130)
