[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseCase\<S, Pat, Body\>

Defined in: [types/base.ts:180](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L180)

Case 分支基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Case`](../../interfaces/Case.md)
- [`Case`](../../namespaces/Core/interfaces/Case.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Pat

`Pat` = `unknown`

### Body

`Body` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Case"`

Defined in: [types/base.ts:181](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L181)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### pattern

> `readonly` **pattern**: `Pat`

Defined in: [types/base.ts:182](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L182)

***

### body

> `readonly` **body**: `Body`

Defined in: [types/base.ts:183](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L183)
