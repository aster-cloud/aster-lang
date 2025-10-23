[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Start

Defined in: [types.ts:210](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types.ts#L210)

Start 异步任务基础接口。

## Extends

- [`BaseStart`](../base/interfaces/BaseStart.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:211](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types.ts#L211)

#### Overrides

[`BaseStart`](../base/interfaces/BaseStart.md).[`span`](../base/interfaces/BaseStart.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L35)

#### Inherited from

[`BaseStart`](../base/interfaces/BaseStart.md).[`origin`](../base/interfaces/BaseStart.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L36)

#### Inherited from

[`BaseStart`](../base/interfaces/BaseStart.md).[`file`](../base/interfaces/BaseStart.md#file)

***

### kind

> `readonly` **kind**: `"Start"`

Defined in: [types/base.ts:192](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L192)

#### Inherited from

[`Start`](../namespaces/Core/interfaces/Start.md).[`kind`](../namespaces/Core/interfaces/Start.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:193](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L193)

#### Inherited from

[`Start`](../namespaces/Core/interfaces/Start.md).[`name`](../namespaces/Core/interfaces/Start.md#name)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:194](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L194)

#### Inherited from

[`BaseStart`](../base/interfaces/BaseStart.md).[`expr`](../base/interfaces/BaseStart.md#expr-1)
