[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Func

Defined in: [types.ts:94](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L94)

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

> `readonly` **kind**: `"Func"`

Defined in: [types.ts:95](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L95)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types.ts:96](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L96)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types.ts:97](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L97)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:98](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L98)

***

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:99](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L99)

***

### effects

> `readonly` **effects**: readonly `string`[]

Defined in: [types.ts:100](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L100)

***

### body

> `readonly` **body**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:101](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L101)
