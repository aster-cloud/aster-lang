[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseResult\<S, T\>

Defined in: [types/base.ts:434](https://github.com/wontlost-ltd/aster-lang/blob/48e79f6ef362b201e4e5194e6bb61b83d6b22f3a/src/types/base.ts#L434)

Result 类型基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Result`](../../interfaces/Result.md)
- [`Result`](../../namespaces/Core/interfaces/Result.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/48e79f6ef362b201e4e5194e6bb61b83d6b22f3a/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/48e79f6ef362b201e4e5194e6bb61b83d6b22f3a/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/48e79f6ef362b201e4e5194e6bb61b83d6b22f3a/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Result"`

Defined in: [types/base.ts:435](https://github.com/wontlost-ltd/aster-lang/blob/48e79f6ef362b201e4e5194e6bb61b83d6b22f3a/src/types/base.ts#L435)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### ok

> `readonly` **ok**: `T`

Defined in: [types/base.ts:436](https://github.com/wontlost-ltd/aster-lang/blob/48e79f6ef362b201e4e5194e6bb61b83d6b22f3a/src/types/base.ts#L436)

***

### err

> `readonly` **err**: `T`

Defined in: [types/base.ts:437](https://github.com/wontlost-ltd/aster-lang/blob/48e79f6ef362b201e4e5194e6bb61b83d6b22f3a/src/types/base.ts#L437)
