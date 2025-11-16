[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Result

Defined in: [types.ts:410](https://github.com/wontlost-ltd/aster-lang/blob/23eb6a773fc1e04c4b93f4380fd9d1f84e496756/src/types.ts#L410)

Result 类型基础接口。

## Extends

- [`BaseResult`](../base/interfaces/BaseResult.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:411](https://github.com/wontlost-ltd/aster-lang/blob/23eb6a773fc1e04c4b93f4380fd9d1f84e496756/src/types.ts#L411)

#### Overrides

[`BaseResult`](../base/interfaces/BaseResult.md).[`span`](../base/interfaces/BaseResult.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/23eb6a773fc1e04c4b93f4380fd9d1f84e496756/src/types/base.ts#L42)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`origin`](../base/interfaces/BaseResult.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/23eb6a773fc1e04c4b93f4380fd9d1f84e496756/src/types/base.ts#L43)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`file`](../base/interfaces/BaseResult.md#file)

***

### kind

> `readonly` **kind**: `"Result"`

Defined in: [types/base.ts:476](https://github.com/wontlost-ltd/aster-lang/blob/23eb6a773fc1e04c4b93f4380fd9d1f84e496756/src/types/base.ts#L476)

#### Inherited from

[`Result`](../namespaces/Core/interfaces/Result.md).[`kind`](../namespaces/Core/interfaces/Result.md#kind)

***

### ok

> `readonly` **ok**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:477](https://github.com/wontlost-ltd/aster-lang/blob/23eb6a773fc1e04c4b93f4380fd9d1f84e496756/src/types/base.ts#L477)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`ok`](../base/interfaces/BaseResult.md#ok)

***

### err

> `readonly` **err**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:478](https://github.com/wontlost-ltd/aster-lang/blob/23eb6a773fc1e04c4b93f4380fd9d1f84e496756/src/types/base.ts#L478)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`err`](../base/interfaces/BaseResult.md#err)
