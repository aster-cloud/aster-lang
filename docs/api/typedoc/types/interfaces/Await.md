[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Await

Defined in: [types.ts:254](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types.ts#L254)

Await 表达式基础接口。

## Extends

- [`BaseAwait`](../base/interfaces/BaseAwait.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:255](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types.ts#L255)

#### Overrides

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`span`](../base/interfaces/BaseAwait.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L42)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`origin`](../base/interfaces/BaseAwait.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L43)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`file`](../base/interfaces/BaseAwait.md#file)

***

### kind

> `readonly` **kind**: `"Await"`

Defined in: [types/base.ts:382](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L382)

#### Inherited from

[`Await`](../namespaces/Core/interfaces/Await.md).[`kind`](../namespaces/Core/interfaces/Await.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:383](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L383)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`expr`](../base/interfaces/BaseAwait.md#expr-1)
