[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Match

Defined in: [types.ts:106](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types.ts#L106)

Match 语句基础接口。

## Extends

- [`BaseMatch`](../base/interfaces/BaseMatch.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md), [`Case`](Case.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L32)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`span`](../base/interfaces/BaseMatch.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L33)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`origin`](../base/interfaces/BaseMatch.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L34)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`file`](../base/interfaces/BaseMatch.md#file)

***

### kind

> `readonly` **kind**: `"Match"`

Defined in: [types/base.ts:171](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L171)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`kind`](../base/interfaces/BaseMatch.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:172](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L172)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`expr`](../base/interfaces/BaseMatch.md#expr-1)

***

### cases

> `readonly` **cases**: readonly [`Case`](Case.md)[]

Defined in: [types/base.ts:173](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L173)

#### Inherited from

[`BaseMatch`](../base/interfaces/BaseMatch.md).[`cases`](../base/interfaces/BaseMatch.md#cases)
