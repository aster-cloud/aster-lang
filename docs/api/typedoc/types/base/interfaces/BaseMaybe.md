[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseMaybe\<S, T\>

Defined in: [types/base.ts:459](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L459)

Maybe 类型基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Maybe`](../../interfaces/Maybe.md)
- [`Maybe`](../../namespaces/Core/interfaces/Maybe.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Maybe"`

Defined in: [types/base.ts:460](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L460)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### type

> `readonly` **type**: `T`

Defined in: [types/base.ts:461](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L461)
