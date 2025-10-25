[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Await

Defined in: [types.ts:508](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types.ts#L508)

Await 表达式基础接口。

## Extends

- [`BaseAwait`](../../../base/interfaces/BaseAwait.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L41)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`span`](../../../base/interfaces/BaseAwait.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L42)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`origin`](../../../base/interfaces/BaseAwait.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L43)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`file`](../../../base/interfaces/BaseAwait.md#file)

***

### kind

> `readonly` **kind**: `"Await"`

Defined in: [types/base.ts:382](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L382)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`kind`](../../../base/interfaces/BaseAwait.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:383](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L383)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`expr`](../../../base/interfaces/BaseAwait.md#expr-1)
