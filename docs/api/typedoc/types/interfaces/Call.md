[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Call

Defined in: [types.ts:246](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L246)

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

> `readonly` **kind**: `"Call"`

Defined in: [types.ts:247](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L247)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### target

> `readonly` **target**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:248](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L248)

***

### args

> `readonly` **args**: readonly [`Expression`](../type-aliases/Expression.md)[]

Defined in: [types.ts:249](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L249)
