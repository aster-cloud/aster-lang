[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypeApp

Defined in: [types.ts:416](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types.ts#L416)

类型应用基础接口。

## Extends

- [`BaseTypeApp`](../base/interfaces/BaseTypeApp.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:417](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types.ts#L417)

#### Overrides

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`span`](../base/interfaces/BaseTypeApp.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L42)

#### Inherited from

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`origin`](../base/interfaces/BaseTypeApp.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L43)

#### Inherited from

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`file`](../base/interfaces/BaseTypeApp.md#file)

***

### kind

> `readonly` **kind**: `"TypeApp"`

Defined in: [types/base.ts:451](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L451)

#### Inherited from

[`TypeApp`](../namespaces/Core/interfaces/TypeApp.md).[`kind`](../namespaces/Core/interfaces/TypeApp.md#kind)

***

### base

> `readonly` **base**: `string`

Defined in: [types/base.ts:452](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L452)

#### Inherited from

[`TypeApp`](../namespaces/Core/interfaces/TypeApp.md).[`base`](../namespaces/Core/interfaces/TypeApp.md#base)

***

### args

> `readonly` **args**: readonly [`Type`](../type-aliases/Type.md)[]

Defined in: [types/base.ts:453](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L453)

#### Inherited from

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`args`](../base/interfaces/BaseTypeApp.md#args)
