[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:239](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types.ts#L239)

Lambda 表达式基础接口。

## Extends

- [`BaseLambda`](../base/interfaces/BaseLambda.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md), [`Block`](Block.md)\>

## Properties

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:240](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types.ts#L240)

#### Overrides

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`retType`](../base/interfaces/BaseLambda.md#rettype)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types/base.ts#L32)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`span`](../base/interfaces/BaseLambda.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types/base.ts#L33)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`origin`](../base/interfaces/BaseLambda.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types/base.ts#L34)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`file`](../base/interfaces/BaseLambda.md#file)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types/base.ts:312](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types/base.ts#L312)

#### Inherited from

[`Lambda`](../namespaces/Core/interfaces/Lambda.md).[`kind`](../namespaces/Core/interfaces/Lambda.md#kind)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](../base/interfaces/BaseParameter.md)\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:313](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types/base.ts#L313)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`params`](../base/interfaces/BaseLambda.md#params)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:315](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types/base.ts#L315)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`body`](../base/interfaces/BaseLambda.md#body)
