[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:551](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types.ts#L551)

Lambda 表达式基础接口。

## Extends

- [`BaseLambda`](../../../base/interfaces/BaseLambda.md)\<[`Origin`](../../../interfaces/Origin.md), [`Type`](../type-aliases/Type.md), [`Block`](Block.md)\>

## Properties

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:552](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types.ts#L552)

***

### captures?

> `readonly` `optional` **captures**: readonly `string`[]

Defined in: [types.ts:553](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types.ts#L553)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L41)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`span`](../../../base/interfaces/BaseLambda.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L42)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`origin`](../../../base/interfaces/BaseLambda.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L43)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`file`](../../../base/interfaces/BaseLambda.md#file)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types/base.ts:365](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L365)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`kind`](../../../base/interfaces/BaseLambda.md#kind)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](../../../base/interfaces/BaseParameter.md)\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:366](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L366)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`params`](../../../base/interfaces/BaseLambda.md#params)

***

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:367](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L367)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`retType`](../../../base/interfaces/BaseLambda.md#rettype)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:368](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L368)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`body`](../../../base/interfaces/BaseLambda.md#body)
