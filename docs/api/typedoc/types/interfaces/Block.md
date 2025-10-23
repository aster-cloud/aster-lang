[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Block

Defined in: [types.ts:178](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types.ts#L178)

Block 基础接口。

## Extends

- [`BaseBlock`](../base/interfaces/BaseBlock.md)\<[`Span`](Span.md), [`Statement`](../type-aliases/Statement.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:179](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types.ts#L179)

#### Overrides

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`span`](../base/interfaces/BaseBlock.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L35)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`origin`](../base/interfaces/BaseBlock.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L36)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`file`](../base/interfaces/BaseBlock.md#file)

***

### kind

> `readonly` **kind**: `"Block"`

Defined in: [types/base.ts:148](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L148)

#### Inherited from

[`Block`](../namespaces/Core/interfaces/Block.md).[`kind`](../namespaces/Core/interfaces/Block.md#kind)

***

### statements

> `readonly` **statements**: readonly [`Statement`](../type-aliases/Statement.md)[]

Defined in: [types/base.ts:149](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L149)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`statements`](../base/interfaces/BaseBlock.md#statements)
