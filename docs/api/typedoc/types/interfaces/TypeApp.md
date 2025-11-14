[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypeApp

Defined in: [types.ts:398](https://github.com/wontlost-ltd/aster-lang/blob/d0422831ed603278d294a40664f5f8949bd9d592/src/types.ts#L398)

类型应用基础接口。

## Extends

- [`BaseTypeApp`](../base/interfaces/BaseTypeApp.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:399](https://github.com/wontlost-ltd/aster-lang/blob/d0422831ed603278d294a40664f5f8949bd9d592/src/types.ts#L399)

#### Overrides

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`span`](../base/interfaces/BaseTypeApp.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/d0422831ed603278d294a40664f5f8949bd9d592/src/types/base.ts#L42)

#### Inherited from

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`origin`](../base/interfaces/BaseTypeApp.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/d0422831ed603278d294a40664f5f8949bd9d592/src/types/base.ts#L43)

#### Inherited from

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`file`](../base/interfaces/BaseTypeApp.md#file)

***

### kind

> `readonly` **kind**: `"TypeApp"`

Defined in: [types/base.ts:451](https://github.com/wontlost-ltd/aster-lang/blob/d0422831ed603278d294a40664f5f8949bd9d592/src/types/base.ts#L451)

#### Inherited from

[`TypeApp`](../namespaces/Core/interfaces/TypeApp.md).[`kind`](../namespaces/Core/interfaces/TypeApp.md#kind)

***

### base

> `readonly` **base**: `string`

Defined in: [types/base.ts:452](https://github.com/wontlost-ltd/aster-lang/blob/d0422831ed603278d294a40664f5f8949bd9d592/src/types/base.ts#L452)

#### Inherited from

[`TypeApp`](../namespaces/Core/interfaces/TypeApp.md).[`base`](../namespaces/Core/interfaces/TypeApp.md#base)

***

### args

> `readonly` **args**: readonly [`Type`](../type-aliases/Type.md)[]

Defined in: [types/base.ts:453](https://github.com/wontlost-ltd/aster-lang/blob/d0422831ed603278d294a40664f5f8949bd9d592/src/types/base.ts#L453)

#### Inherited from

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`args`](../base/interfaces/BaseTypeApp.md#args)
