[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: FuncType

Defined in: [types.ts:422](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types.ts#L422)

函数类型基础接口。

## Extends

- [`BaseFuncType`](../base/interfaces/BaseFuncType.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:423](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types.ts#L423)

#### Overrides

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`span`](../base/interfaces/BaseFuncType.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L42)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`origin`](../base/interfaces/BaseFuncType.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L43)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`file`](../base/interfaces/BaseFuncType.md#file)

***

### kind

> `readonly` **kind**: `"FuncType"`

Defined in: [types/base.ts:502](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L502)

#### Inherited from

[`FuncType`](../namespaces/Core/interfaces/FuncType.md).[`kind`](../namespaces/Core/interfaces/FuncType.md#kind)

***

### params

> `readonly` **params**: readonly [`Type`](../type-aliases/Type.md)[]

Defined in: [types/base.ts:503](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L503)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`params`](../base/interfaces/BaseFuncType.md#params)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:504](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L504)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`ret`](../base/interfaces/BaseFuncType.md#ret)
