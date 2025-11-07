[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseOption\<S, T\>

Defined in: [types/base.ts:426](https://github.com/wontlost-ltd/aster-lang/blob/15240bb8979a8fe7b7b1bfa8309f1a78ca9fb35b/src/types/base.ts#L426)

Option 类型基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Option`](../../interfaces/Option.md)
- [`Option`](../../namespaces/Core/interfaces/Option.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/15240bb8979a8fe7b7b1bfa8309f1a78ca9fb35b/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/15240bb8979a8fe7b7b1bfa8309f1a78ca9fb35b/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/15240bb8979a8fe7b7b1bfa8309f1a78ca9fb35b/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Option"`

Defined in: [types/base.ts:427](https://github.com/wontlost-ltd/aster-lang/blob/15240bb8979a8fe7b7b1bfa8309f1a78ca9fb35b/src/types/base.ts#L427)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### type

> `readonly` **type**: `T`

Defined in: [types/base.ts:428](https://github.com/wontlost-ltd/aster-lang/blob/15240bb8979a8fe7b7b1bfa8309f1a78ca9fb35b/src/types/base.ts#L428)
