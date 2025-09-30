[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Func

Defined in: [types.ts:95](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L95)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### span?

> `readonly` `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:62](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L62)

#### Inherited from

[`AstNode`](AstNode.md).[`span`](AstNode.md#span)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types.ts:63](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L63)

#### Inherited from

[`AstNode`](AstNode.md).[`file`](AstNode.md#file)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types.ts:96](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L96)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types.ts:97](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L97)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types.ts:98](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L98)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:99](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L99)

***

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:100](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L100)

***

### effects

> `readonly` **effects**: readonly `string`[]

Defined in: [types.ts:101](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L101)

***

### body

> `readonly` **body**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:102](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L102)
