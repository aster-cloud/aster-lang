[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Case

Defined in: [types.ts:149](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L149)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### span?

> `readonly` `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:62](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L62)

#### Inherited from

[`AstNode`](AstNode.md).[`span`](AstNode.md#span)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types.ts:63](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L63)

#### Inherited from

[`AstNode`](AstNode.md).[`file`](AstNode.md#file)

***

### kind

> `readonly` **kind**: `"Case"`

Defined in: [types.ts:150](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L150)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### pattern

> `readonly` **pattern**: [`Pattern`](../type-aliases/Pattern.md)

Defined in: [types.ts:151](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L151)

***

### body

> `readonly` **body**: [`Block`](Block.md) \| [`Return`](Return.md)

Defined in: [types.ts:152](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L152)
