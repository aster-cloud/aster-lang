[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Match

Defined in: [types.ts:218](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types.ts#L218)

Match 语句基础接口。

## Extends

- [`BaseMatch`](../base/interfaces/BaseMatch.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md), [`Case`](Case.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:219](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types.ts#L219)

#### Overrides

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`span`](../base/interfaces/BaseMatch.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L42)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`origin`](../base/interfaces/BaseMatch.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L43)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`file`](../base/interfaces/BaseMatch.md#file)

***

### kind

> `readonly` **kind**: `"Match"`

Defined in: [types/base.ts:181](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L181)

#### Inherited from

[`Match`](../namespaces/Core/interfaces/Match.md).[`kind`](../namespaces/Core/interfaces/Match.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:182](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L182)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`expr`](../base/interfaces/BaseMatch.md#expr-1)

***

### cases

> `readonly` **cases**: readonly [`Case`](Case.md)[]

Defined in: [types/base.ts:183](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L183)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`cases`](../base/interfaces/BaseMatch.md#cases)
