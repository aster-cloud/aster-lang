[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Err

Defined in: [types.ts:501](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L501)

Err 表达式基础接口。

## Extends

- [`BaseErr`](../../../base/interfaces/BaseErr.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L34)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`span`](../../../base/interfaces/BaseErr.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L35)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`origin`](../../../base/interfaces/BaseErr.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L36)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`file`](../../../base/interfaces/BaseErr.md#file)

***

### kind

> `readonly` **kind**: `"Err"`

Defined in: [types/base.ts:352](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L352)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`kind`](../../../base/interfaces/BaseErr.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:353](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L353)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`expr`](../../../base/interfaces/BaseErr.md#expr-1)
