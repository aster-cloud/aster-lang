[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Await

Defined in: [types.ts:507](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L507)

Await 表达式基础接口。

## Extends

- [`BaseAwait`](../../../base/interfaces/BaseAwait.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L34)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`span`](../../../base/interfaces/BaseAwait.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L35)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`origin`](../../../base/interfaces/BaseAwait.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L36)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`file`](../../../base/interfaces/BaseAwait.md#file)

***

### kind

> `readonly` **kind**: `"Await"`

Defined in: [types/base.ts:375](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L375)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`kind`](../../../base/interfaces/BaseAwait.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:376](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L376)

#### Inherited from

[`BaseAwait`](../../../base/interfaces/BaseAwait.md).[`expr`](../../../base/interfaces/BaseAwait.md#expr-1)
