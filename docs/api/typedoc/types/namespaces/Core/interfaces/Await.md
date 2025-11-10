[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Await

Defined in: [types.ts:557](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types.ts#L557)

Await 表达式基础接口。

## Extends

- [`BaseAwait`](../../../base/interfaces/BaseAwait.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L41)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`span`](../../../base/interfaces/BaseAwait.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L42)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`origin`](../../../base/interfaces/BaseAwait.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L43)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`file`](../../../base/interfaces/BaseAwait.md#file)

***

### kind

> `readonly` **kind**: `"Await"`

Defined in: [types/base.ts:422](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L422)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`kind`](../../../base/interfaces/BaseAwait.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:423](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L423)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`expr`](../../../base/interfaces/BaseAwait.md#expr-1)
