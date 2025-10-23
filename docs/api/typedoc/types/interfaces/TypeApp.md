[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypeApp

Defined in: [types.ts:361](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types.ts#L361)

类型应用基础接口。

## Extends

- [`BaseTypeApp`](../base/interfaces/BaseTypeApp.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:362](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types.ts#L362)

#### Overrides

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`span`](../base/interfaces/BaseTypeApp.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L35)

#### Inherited from

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`origin`](../base/interfaces/BaseTypeApp.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L36)

#### Inherited from

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`file`](../base/interfaces/BaseTypeApp.md#file)

***

### kind

> `readonly` **kind**: `"TypeApp"`

Defined in: [types/base.ts:403](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L403)

#### Inherited from

[`TypeApp`](../namespaces/Core/interfaces/TypeApp.md).[`kind`](../namespaces/Core/interfaces/TypeApp.md#kind)

***

### base

> `readonly` **base**: `string`

Defined in: [types/base.ts:404](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L404)

#### Inherited from

[`TypeApp`](../namespaces/Core/interfaces/TypeApp.md).[`base`](../namespaces/Core/interfaces/TypeApp.md#base)

***

### args

> `readonly` **args**: readonly [`Type`](../type-aliases/Type.md)[]

Defined in: [types/base.ts:405](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L405)

#### Inherited from

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`args`](../base/interfaces/BaseTypeApp.md#args)
