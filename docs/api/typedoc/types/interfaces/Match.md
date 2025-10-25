[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Match

Defined in: [types.ts:202](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types.ts#L202)

Match 语句基础接口。

## Extends

- [`BaseMatch`](../base/interfaces/BaseMatch.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md), [`Case`](Case.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:203](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types.ts#L203)

#### Overrides

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`span`](../base/interfaces/BaseMatch.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L42)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`origin`](../base/interfaces/BaseMatch.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L43)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`file`](../base/interfaces/BaseMatch.md#file)

***

### kind

> `readonly` **kind**: `"Match"`

Defined in: [types/base.ts:181](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L181)

#### Inherited from

[`Match`](../namespaces/Core/interfaces/Match.md).[`kind`](../namespaces/Core/interfaces/Match.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:182](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L182)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`expr`](../base/interfaces/BaseMatch.md#expr-1)

***

### cases

> `readonly` **cases**: readonly [`Case`](Case.md)[]

Defined in: [types/base.ts:183](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L183)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`cases`](../base/interfaces/BaseMatch.md#cases)
