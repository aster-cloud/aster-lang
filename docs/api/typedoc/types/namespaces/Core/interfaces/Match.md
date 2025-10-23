[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Match

Defined in: [types.ts:443](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L443)

Match 语句基础接口。

## Extends

- [`BaseMatch`](../../../base/interfaces/BaseMatch.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md), [`Case`](Case.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L34)

#### Inherited from

[`BaseMatch`](../../../base/interfaces/BaseMatch.md).[`span`](../../../base/interfaces/BaseMatch.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L35)

#### Inherited from

[`BaseMatch`](../../../base/interfaces/BaseMatch.md).[`origin`](../../../base/interfaces/BaseMatch.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L36)

#### Inherited from

[`BaseMatch`](../../../base/interfaces/BaseMatch.md).[`file`](../../../base/interfaces/BaseMatch.md#file)

***

### kind

> `readonly` **kind**: `"Match"`

Defined in: [types/base.ts:174](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L174)

#### Inherited from

[`BaseMatch`](../../../base/interfaces/BaseMatch.md).[`kind`](../../../base/interfaces/BaseMatch.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:175](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L175)

#### Inherited from

[`BaseMatch`](../../../base/interfaces/BaseMatch.md).[`expr`](../../../base/interfaces/BaseMatch.md#expr-1)

***

### cases

> `readonly` **cases**: readonly [`Case`](Case.md)[]

Defined in: [types/base.ts:176](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L176)

#### Inherited from

[`BaseMatch`](../../../base/interfaces/BaseMatch.md).[`cases`](../../../base/interfaces/BaseMatch.md#cases)
