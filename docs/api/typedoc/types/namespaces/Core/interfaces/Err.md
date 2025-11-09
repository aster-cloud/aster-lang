[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Err

Defined in: [types.ts:551](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types.ts#L551)

Err 表达式基础接口。

## Extends

- [`BaseErr`](../../../base/interfaces/BaseErr.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L41)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`span`](../../../base/interfaces/BaseErr.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L42)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`origin`](../../../base/interfaces/BaseErr.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L43)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`file`](../../../base/interfaces/BaseErr.md#file)

***

### kind

> `readonly` **kind**: `"Err"`

Defined in: [types/base.ts:399](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L399)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`kind`](../../../base/interfaces/BaseErr.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:400](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L400)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`expr`](../../../base/interfaces/BaseErr.md#expr-1)
