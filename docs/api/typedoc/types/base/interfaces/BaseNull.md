[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseNull\<S\>

Defined in: [types/base.ts:307](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L307)

Null 字面量基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Null`](../../interfaces/Null.md)
- [`Null`](../../namespaces/Core/interfaces/Null.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Null"`

Defined in: [types/base.ts:308](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L308)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)
