[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseLambda\<S, T, Block\>

Defined in: [types/base.ts:364](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L364)

Lambda 表达式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Lambda`](../../interfaces/Lambda.md)
- [`Lambda`](../../namespaces/Core/interfaces/Lambda.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

### Block

`Block` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types/base.ts:365](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L365)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](BaseParameter.md)\<`T`\>[]

Defined in: [types/base.ts:366](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L366)

***

### retType

> `readonly` **retType**: `T`

Defined in: [types/base.ts:367](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L367)

***

### body

> `readonly` **body**: `Block`

Defined in: [types/base.ts:368](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L368)
