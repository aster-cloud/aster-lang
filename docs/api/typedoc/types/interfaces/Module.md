[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Module

Defined in: [types.ts:66](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L66)

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

> `readonly` **kind**: `"Module"`

Defined in: [types.ts:67](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L67)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### name

> `readonly` **name**: `null` \| `string`

Defined in: [types.ts:68](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L68)

***

### decls

> `readonly` **decls**: readonly [`Declaration`](../type-aliases/Declaration.md)[]

Defined in: [types.ts:69](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L69)
