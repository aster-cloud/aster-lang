[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:290](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types.ts#L290)

Lambda 表达式基础接口。

## Extends

- [`BaseLambda`](../base/interfaces/BaseLambda.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md), [`Block`](Block.md)\>

## Properties

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:291](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types.ts#L291)

#### Overrides

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`retType`](../base/interfaces/BaseLambda.md#rettype)

***

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:292](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types.ts#L292)

#### Overrides

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`span`](../base/interfaces/BaseLambda.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types/base.ts#L42)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`origin`](../base/interfaces/BaseLambda.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types/base.ts#L43)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`file`](../base/interfaces/BaseLambda.md#file)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types/base.ts:324](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types/base.ts#L324)

#### Inherited from

[`Lambda`](../namespaces/Core/interfaces/Lambda.md).[`kind`](../namespaces/Core/interfaces/Lambda.md#kind)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](../base/interfaces/BaseParameter.md)\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:325](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types/base.ts#L325)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`params`](../base/interfaces/BaseLambda.md#params)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:327](https://github.com/wontlost-ltd/aster-lang/blob/09683d3c2c34fd40d20a4508d35c1b1f4208abbc/src/types/base.ts#L327)

#### Inherited from

[`BaseLambda`](../base/interfaces/BaseLambda.md).[`body`](../base/interfaces/BaseLambda.md#body)
