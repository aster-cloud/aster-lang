[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:159](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types.ts#L159)

Lambda 表达式基础接口。

## Extends

- [`BaseLambda`](../base/interfaces/BaseLambda.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md), [`Block`](Block.md)\>

## Properties

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:160](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types.ts#L160)

#### Overrides

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`retType`](../base/interfaces/BaseLambda.md#rettype)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L32)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`span`](../base/interfaces/BaseLambda.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L33)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`origin`](../base/interfaces/BaseLambda.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L34)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`file`](../base/interfaces/BaseLambda.md#file)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types/base.ts:311](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L311)

#### Inherited from

[`Lambda`](../namespaces/Core/interfaces/Lambda.md).[`kind`](../namespaces/Core/interfaces/Lambda.md#kind)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](../base/interfaces/BaseParameter.md)\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:312](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L312)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`params`](../base/interfaces/BaseLambda.md#params)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:314](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L314)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`body`](../base/interfaces/BaseLambda.md#body)
