[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: FuncType

Defined in: [types.ts:331](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L331)

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

> `readonly` **kind**: `"FuncType"`

Defined in: [types.ts:332](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L332)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### params

> `readonly` **params**: readonly [`Type`](../type-aliases/Type.md)[]

Defined in: [types.ts:333](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L333)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:334](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L334)
