[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Call

Defined in: [types.ts:243](https://github.com/wontlost-ltd/aster-lang/blob/f3be429a3dd4fa3b3fa1ca69bb6ae12cde8a35f5/src/types.ts#L243)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### span?

> `readonly` `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:62](https://github.com/wontlost-ltd/aster-lang/blob/f3be429a3dd4fa3b3fa1ca69bb6ae12cde8a35f5/src/types.ts#L62)

#### Inherited from

[`AstNode`](AstNode.md).[`span`](AstNode.md#span)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types.ts:63](https://github.com/wontlost-ltd/aster-lang/blob/f3be429a3dd4fa3b3fa1ca69bb6ae12cde8a35f5/src/types.ts#L63)

#### Inherited from

[`AstNode`](AstNode.md).[`file`](AstNode.md#file)

***

### kind

> `readonly` **kind**: `"Call"`

Defined in: [types.ts:244](https://github.com/wontlost-ltd/aster-lang/blob/f3be429a3dd4fa3b3fa1ca69bb6ae12cde8a35f5/src/types.ts#L244)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### target

> `readonly` **target**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:245](https://github.com/wontlost-ltd/aster-lang/blob/f3be429a3dd4fa3b3fa1ca69bb6ae12cde8a35f5/src/types.ts#L245)

***

### args

> `readonly` **args**: readonly [`Expression`](../type-aliases/Expression.md)[]

Defined in: [types.ts:246](https://github.com/wontlost-ltd/aster-lang/blob/f3be429a3dd4fa3b3fa1ca69bb6ae12cde8a35f5/src/types.ts#L246)
