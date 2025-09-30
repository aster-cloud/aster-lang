[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Match

Defined in: [types.ts:143](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L143)

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

> `readonly` **kind**: `"Match"`

Defined in: [types.ts:144](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L144)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:145](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L145)

***

### cases

> `readonly` **cases**: readonly [`Case`](Case.md)[]

Defined in: [types.ts:146](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L146)
