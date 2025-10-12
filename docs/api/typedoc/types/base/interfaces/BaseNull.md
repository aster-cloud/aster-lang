[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseNull\<S\>

Defined in: [types/base.ts:295](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L295)

Null 字面量基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Null`](../../interfaces/Null.md)
- [`Null`](../../namespaces/Core/interfaces/Null.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Null"`

Defined in: [types/base.ts:296](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L296)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)
