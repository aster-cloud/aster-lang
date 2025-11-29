[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Await

Defined in: [types.ts:282](https://github.com/wontlost-ltd/aster-lang/blob/e4b7a41c141fab64c03c5017e7bd6371419cde5c/src/types.ts#L282)

Await 表达式基础接口。

## Extends

- [`BaseAwait`](../base/interfaces/BaseAwait.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:283](https://github.com/wontlost-ltd/aster-lang/blob/e4b7a41c141fab64c03c5017e7bd6371419cde5c/src/types.ts#L283)

#### Overrides

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`span`](../base/interfaces/BaseAwait.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/e4b7a41c141fab64c03c5017e7bd6371419cde5c/src/types/base.ts#L42)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`origin`](../base/interfaces/BaseAwait.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/e4b7a41c141fab64c03c5017e7bd6371419cde5c/src/types/base.ts#L43)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`file`](../base/interfaces/BaseAwait.md#file)

***

### kind

> `readonly` **kind**: `"Await"`

Defined in: [types/base.ts:423](https://github.com/wontlost-ltd/aster-lang/blob/e4b7a41c141fab64c03c5017e7bd6371419cde5c/src/types/base.ts#L423)

#### Inherited from

[`Await`](../namespaces/Core/interfaces/Await.md).[`kind`](../namespaces/Core/interfaces/Await.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:424](https://github.com/wontlost-ltd/aster-lang/blob/e4b7a41c141fab64c03c5017e7bd6371419cde5c/src/types/base.ts#L424)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`expr`](../base/interfaces/BaseAwait.md#expr-1)
