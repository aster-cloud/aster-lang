[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Match

Defined in: [types.ts:146](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L146)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### span?

> `readonly` `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:64](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L64)

#### Inherited from

[`AstNode`](AstNode.md).[`span`](AstNode.md#span)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types.ts:65](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L65)

#### Inherited from

[`AstNode`](AstNode.md).[`file`](AstNode.md#file)

***

### kind

> `readonly` **kind**: `"Match"`

Defined in: [types.ts:147](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L147)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:148](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L148)

***

### cases

> `readonly` **cases**: readonly [`Case`](Case.md)[]

Defined in: [types.ts:149](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L149)
