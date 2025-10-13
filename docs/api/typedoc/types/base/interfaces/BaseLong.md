[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseLong\<S\>

Defined in: [types/base.ts:271](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L271)

Long 字面量基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Long`](../../interfaces/Long.md)
- [`Long`](../../namespaces/Core/interfaces/Long.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Long"`

Defined in: [types/base.ts:272](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L272)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `number`

Defined in: [types/base.ts:273](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L273)
