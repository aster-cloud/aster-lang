[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Module

Defined in: [types.ts:68](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L68)

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

> `readonly` **kind**: `"Module"`

Defined in: [types.ts:69](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L69)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### name

> `readonly` **name**: `null` \| `string`

Defined in: [types.ts:70](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L70)

***

### decls

> `readonly` **decls**: readonly [`Declaration`](../type-aliases/Declaration.md)[]

Defined in: [types.ts:71](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L71)
