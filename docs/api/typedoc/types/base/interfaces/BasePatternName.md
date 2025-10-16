[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BasePatternName\<S\>

Defined in: [types/base.ts:227](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L227)

名称模式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`PatternName`](../../interfaces/PatternName.md)
- [`PatName`](../../namespaces/Core/interfaces/PatName.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

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

> `readonly` **kind**: `"PatternName"` \| `"PatName"`

Defined in: [types/base.ts:228](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L228)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:229](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L229)
