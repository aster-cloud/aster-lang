[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: If

Defined in: [types.ts:136](https://github.com/wontlost-ltd/aster-lang/blob/4267a217b736c6a8661d2a198c0f0014f00e7248/src/types.ts#L136)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### span?

> `readonly` `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:62](https://github.com/wontlost-ltd/aster-lang/blob/4267a217b736c6a8661d2a198c0f0014f00e7248/src/types.ts#L62)

#### Inherited from

[`AstNode`](AstNode.md).[`span`](AstNode.md#span)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types.ts:63](https://github.com/wontlost-ltd/aster-lang/blob/4267a217b736c6a8661d2a198c0f0014f00e7248/src/types.ts#L63)

#### Inherited from

[`AstNode`](AstNode.md).[`file`](AstNode.md#file)

***

### kind

> `readonly` **kind**: `"If"`

Defined in: [types.ts:137](https://github.com/wontlost-ltd/aster-lang/blob/4267a217b736c6a8661d2a198c0f0014f00e7248/src/types.ts#L137)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:138](https://github.com/wontlost-ltd/aster-lang/blob/4267a217b736c6a8661d2a198c0f0014f00e7248/src/types.ts#L138)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types.ts:139](https://github.com/wontlost-ltd/aster-lang/blob/4267a217b736c6a8661d2a198c0f0014f00e7248/src/types.ts#L139)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:140](https://github.com/wontlost-ltd/aster-lang/blob/4267a217b736c6a8661d2a198c0f0014f00e7248/src/types.ts#L140)
