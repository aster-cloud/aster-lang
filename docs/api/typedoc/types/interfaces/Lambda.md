[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:318](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types.ts#L318)

Lambda 表达式基础接口。

## Extends

- [`BaseLambda`](../base/interfaces/BaseLambda.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md), [`Block`](Block.md)\>

## Properties

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:319](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types.ts#L319)

#### Overrides

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`retType`](../base/interfaces/BaseLambda.md#rettype)

***

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:320](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types.ts#L320)

#### Overrides

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`span`](../base/interfaces/BaseLambda.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types/base.ts#L42)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`origin`](../base/interfaces/BaseLambda.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types/base.ts#L43)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`file`](../base/interfaces/BaseLambda.md#file)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types/base.ts:365](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types/base.ts#L365)

#### Inherited from

[`Lambda`](../namespaces/Core/interfaces/Lambda.md).[`kind`](../namespaces/Core/interfaces/Lambda.md#kind)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](../base/interfaces/BaseParameter.md)\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:366](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types/base.ts#L366)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`params`](../base/interfaces/BaseLambda.md#params)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:368](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types/base.ts#L368)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`body`](../base/interfaces/BaseLambda.md#body)
