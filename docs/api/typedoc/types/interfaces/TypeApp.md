[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypeApp

Defined in: [types.ts:363](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types.ts#L363)

类型应用基础接口。

## Extends

- [`BaseTypeApp`](../base/interfaces/BaseTypeApp.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:364](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types.ts#L364)

#### Overrides

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`span`](../base/interfaces/BaseTypeApp.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types/base.ts#L42)

#### Inherited from

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`origin`](../base/interfaces/BaseTypeApp.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types/base.ts#L43)

#### Inherited from

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`file`](../base/interfaces/BaseTypeApp.md#file)

***

### kind

> `readonly` **kind**: `"TypeApp"`

Defined in: [types/base.ts:410](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types/base.ts#L410)

#### Inherited from

[`TypeApp`](../namespaces/Core/interfaces/TypeApp.md).[`kind`](../namespaces/Core/interfaces/TypeApp.md#kind)

***

### base

> `readonly` **base**: `string`

Defined in: [types/base.ts:411](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types/base.ts#L411)

#### Inherited from

[`TypeApp`](../namespaces/Core/interfaces/TypeApp.md).[`base`](../namespaces/Core/interfaces/TypeApp.md#base)

***

### args

> `readonly` **args**: readonly [`Type`](../type-aliases/Type.md)[]

Defined in: [types/base.ts:412](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types/base.ts#L412)

#### Inherited from

[`BaseTypeApp`](../base/interfaces/BaseTypeApp.md).[`args`](../base/interfaces/BaseTypeApp.md#args)
