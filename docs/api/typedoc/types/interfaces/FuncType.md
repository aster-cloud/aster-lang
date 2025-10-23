[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: FuncType

Defined in: [types.ts:385](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types.ts#L385)

函数类型基础接口。

## Extends

- [`BaseFuncType`](../base/interfaces/BaseFuncType.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:386](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types.ts#L386)

#### Overrides

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`span`](../base/interfaces/BaseFuncType.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L35)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`origin`](../base/interfaces/BaseFuncType.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L36)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`file`](../base/interfaces/BaseFuncType.md#file)

***

### kind

> `readonly` **kind**: `"FuncType"`

Defined in: [types/base.ts:454](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L454)

#### Inherited from

[`FuncType`](../namespaces/Core/interfaces/FuncType.md).[`kind`](../namespaces/Core/interfaces/FuncType.md#kind)

***

### params

> `readonly` **params**: readonly [`Type`](../type-aliases/Type.md)[]

Defined in: [types/base.ts:455](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L455)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`params`](../base/interfaces/BaseFuncType.md#params)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:456](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L456)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`ret`](../base/interfaces/BaseFuncType.md#ret)
