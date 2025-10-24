[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:289](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L289)

Lambda 表达式基础接口。

## Extends

- [`BaseLambda`](../base/interfaces/BaseLambda.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md), [`Block`](Block.md)\>

## Properties

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:290](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L290)

#### Overrides

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`retType`](../base/interfaces/BaseLambda.md#rettype)

***

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:291](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L291)

#### Overrides

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`span`](../base/interfaces/BaseLambda.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L35)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`origin`](../base/interfaces/BaseLambda.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L36)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`file`](../base/interfaces/BaseLambda.md#file)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types/base.ts:317](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L317)

#### Inherited from

[`Lambda`](../namespaces/Core/interfaces/Lambda.md).[`kind`](../namespaces/Core/interfaces/Lambda.md#kind)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](../base/interfaces/BaseParameter.md)\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:318](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L318)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`params`](../base/interfaces/BaseLambda.md#params)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:320](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L320)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`body`](../base/interfaces/BaseLambda.md#body)
