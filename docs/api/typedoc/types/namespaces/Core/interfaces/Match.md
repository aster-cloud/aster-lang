[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Match

Defined in: [types.ts:527](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types.ts#L527)

Match 语句基础接口。

## Extends

- [`BaseMatch`](../../../base/interfaces/BaseMatch.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md), [`Case`](Case.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L41)

#### Inherited from

[`BaseMatch`](../../../base/interfaces/BaseMatch.md).[`span`](../../../base/interfaces/BaseMatch.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L42)

#### Inherited from

[`BaseMatch`](../../../base/interfaces/BaseMatch.md).[`origin`](../../../base/interfaces/BaseMatch.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L43)

#### Inherited from

[`BaseMatch`](../../../base/interfaces/BaseMatch.md).[`file`](../../../base/interfaces/BaseMatch.md#file)

***

### kind

> `readonly` **kind**: `"Match"`

Defined in: [types/base.ts:181](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L181)

#### Inherited from

[`BaseMatch`](../../../base/interfaces/BaseMatch.md).[`kind`](../../../base/interfaces/BaseMatch.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:182](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L182)

#### Inherited from

[`BaseMatch`](../../../base/interfaces/BaseMatch.md).[`expr`](../../../base/interfaces/BaseMatch.md#expr-1)

***

### cases

> `readonly` **cases**: readonly [`Case`](Case.md)[]

Defined in: [types/base.ts:183](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L183)

#### Inherited from

[`BaseMatch`](../../../base/interfaces/BaseMatch.md).[`cases`](../../../base/interfaces/BaseMatch.md#cases)
