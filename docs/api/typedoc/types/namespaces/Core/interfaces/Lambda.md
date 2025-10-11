[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:361](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types.ts#L361)

Lambda 表达式基础接口。

## Extends

- [`BaseLambda`](../../../base/interfaces/BaseLambda.md)\<[`Origin`](../../../interfaces/Origin.md), [`Type`](../type-aliases/Type.md), [`Block`](Block.md)\>

## Properties

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:362](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types.ts#L362)

***

### captures?

> `readonly` `optional` **captures**: readonly `string`[]

Defined in: [types.ts:363](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types.ts#L363)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types/base.ts#L32)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`span`](../../../base/interfaces/BaseLambda.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types/base.ts#L33)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`origin`](../../../base/interfaces/BaseLambda.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types/base.ts#L34)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`file`](../../../base/interfaces/BaseLambda.md#file)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types/base.ts:312](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types/base.ts#L312)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`kind`](../../../base/interfaces/BaseLambda.md#kind)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](../../../base/interfaces/BaseParameter.md)\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:313](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types/base.ts#L313)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`params`](../../../base/interfaces/BaseLambda.md#params)

***

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:314](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types/base.ts#L314)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`retType`](../../../base/interfaces/BaseLambda.md#rettype)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:315](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types/base.ts#L315)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`body`](../../../base/interfaces/BaseLambda.md#body)
