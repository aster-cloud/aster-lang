[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Call

Defined in: [types.ts:242](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L242)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### span?

> `readonly` `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:61](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L61)

#### Inherited from

[`AstNode`](AstNode.md).[`span`](AstNode.md#span)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types.ts:62](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L62)

#### Inherited from

[`AstNode`](AstNode.md).[`file`](AstNode.md#file)

***

### kind

> `readonly` **kind**: `"Call"`

Defined in: [types.ts:243](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L243)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### target

> `readonly` **target**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:244](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L244)

***

### args

> `readonly` **args**: readonly [`Expression`](../type-aliases/Expression.md)[]

Defined in: [types.ts:245](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L245)
