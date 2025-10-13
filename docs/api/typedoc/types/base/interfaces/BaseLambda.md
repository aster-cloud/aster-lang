[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseLambda\<S, T, Block\>

Defined in: [types/base.ts:311](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L311)

Lambda 表达式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Lambda`](../../interfaces/Lambda.md)
- [`Lambda`](../../namespaces/Core/interfaces/Lambda.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

### Block

`Block` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types/base.ts:312](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L312)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](BaseParameter.md)\<`T`\>[]

Defined in: [types/base.ts:313](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L313)

***

### retType

> `readonly` **retType**: `T`

Defined in: [types/base.ts:314](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L314)

***

### body

> `readonly` **body**: `Block`

Defined in: [types/base.ts:315](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L315)
