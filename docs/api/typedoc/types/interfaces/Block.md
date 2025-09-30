[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Block

Defined in: [types.ts:110](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L110)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### span?

> `readonly` `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:62](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L62)

#### Inherited from

[`AstNode`](AstNode.md).[`span`](AstNode.md#span)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types.ts:63](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L63)

#### Inherited from

[`AstNode`](AstNode.md).[`file`](AstNode.md#file)

***

### kind

> `readonly` **kind**: `"Block"`

Defined in: [types.ts:111](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L111)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### statements

> `readonly` **statements**: readonly [`Statement`](../type-aliases/Statement.md)[]

Defined in: [types.ts:112](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L112)
