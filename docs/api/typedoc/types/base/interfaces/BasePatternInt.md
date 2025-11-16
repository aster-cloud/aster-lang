[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BasePatternInt\<S\>

Defined in: [types/base.ts:285](https://github.com/wontlost-ltd/aster-lang/blob/23eb6a773fc1e04c4b93f4380fd9d1f84e496756/src/types/base.ts#L285)

整数模式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`PatternInt`](../../interfaces/PatternInt.md)
- [`PatInt`](../../namespaces/Core/interfaces/PatInt.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/23eb6a773fc1e04c4b93f4380fd9d1f84e496756/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/23eb6a773fc1e04c4b93f4380fd9d1f84e496756/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/23eb6a773fc1e04c4b93f4380fd9d1f84e496756/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"PatternInt"` \| `"PatInt"`

Defined in: [types/base.ts:286](https://github.com/wontlost-ltd/aster-lang/blob/23eb6a773fc1e04c4b93f4380fd9d1f84e496756/src/types/base.ts#L286)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `number`

Defined in: [types/base.ts:287](https://github.com/wontlost-ltd/aster-lang/blob/23eb6a773fc1e04c4b93f4380fd9d1f84e496756/src/types/base.ts#L287)
