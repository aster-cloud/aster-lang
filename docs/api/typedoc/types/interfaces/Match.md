[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Match

Defined in: [types.ts:142](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L142)

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

> `readonly` **kind**: `"Match"`

Defined in: [types.ts:143](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L143)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:144](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L144)

***

### cases

> `readonly` **cases**: readonly [`Case`](Case.md)[]

Defined in: [types.ts:145](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L145)
