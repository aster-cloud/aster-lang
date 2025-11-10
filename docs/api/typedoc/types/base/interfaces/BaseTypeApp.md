[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseTypeApp\<S, T\>

Defined in: [types/base.ts:450](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L450)

类型应用基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`TypeApp`](../../interfaces/TypeApp.md)
- [`TypeApp`](../../namespaces/Core/interfaces/TypeApp.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"TypeApp"`

Defined in: [types/base.ts:451](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L451)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### base

> `readonly` **base**: `string`

Defined in: [types/base.ts:452](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L452)

***

### args

> `readonly` **args**: readonly `T`[]

Defined in: [types/base.ts:453](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L453)
