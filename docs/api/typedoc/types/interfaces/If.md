[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: If

Defined in: [types.ts:136](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L136)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### span?

> `readonly` `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:62](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L62)

#### Inherited from

[`AstNode`](AstNode.md).[`span`](AstNode.md#span)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types.ts:63](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L63)

#### Inherited from

[`AstNode`](AstNode.md).[`file`](AstNode.md#file)

***

### kind

> `readonly` **kind**: `"If"`

Defined in: [types.ts:137](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L137)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:138](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L138)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types.ts:139](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L139)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:140](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L140)
