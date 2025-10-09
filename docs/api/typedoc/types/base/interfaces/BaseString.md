[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseString\<S\>

Defined in: [types/base.ts:286](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L286)

字符串字面量基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`String`](../../interfaces/String.md)
- [`String`](../../namespaces/Core/interfaces/String.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"String"`

Defined in: [types/base.ts:287](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L287)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `string`

Defined in: [types/base.ts:288](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L288)
