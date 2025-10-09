[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseTypeVar\<S\>

Defined in: [types/base.ts:388](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L388)

类型变量基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`TypeVar`](../../interfaces/TypeVar.md)
- [`TypeVar`](../../namespaces/Core/interfaces/TypeVar.md)

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

> `readonly` **kind**: `"TypeVar"`

Defined in: [types/base.ts:389](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L389)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:390](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L390)
