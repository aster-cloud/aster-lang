[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Await

Defined in: [types.ts:253](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L253)

Await 表达式基础接口。

## Extends

- [`BaseAwait`](../base/interfaces/BaseAwait.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:254](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L254)

#### Overrides

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`span`](../base/interfaces/BaseAwait.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L35)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`origin`](../base/interfaces/BaseAwait.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L36)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`file`](../base/interfaces/BaseAwait.md#file)

***

### kind

> `readonly` **kind**: `"Await"`

Defined in: [types/base.ts:375](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L375)

#### Inherited from

[`Await`](../namespaces/Core/interfaces/Await.md).[`kind`](../namespaces/Core/interfaces/Await.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:376](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L376)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`expr`](../base/interfaces/BaseAwait.md#expr-1)
