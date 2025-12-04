[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:574](https://github.com/wontlost-ltd/aster-lang/blob/8dd861bbf5cdaedf2f100811eb1228c32bced27f/src/types.ts#L574)

Lambda 表达式基础接口。

## Extends

- [`BaseLambda`](../../../base/interfaces/BaseLambda.md)\<[`Origin`](../../../interfaces/Origin.md), [`Type`](../type-aliases/Type.md), [`Block`](Block.md)\>

## Properties

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:575](https://github.com/wontlost-ltd/aster-lang/blob/8dd861bbf5cdaedf2f100811eb1228c32bced27f/src/types.ts#L575)

***

### captures?

> `readonly` `optional` **captures**: readonly `string`[]

Defined in: [types.ts:576](https://github.com/wontlost-ltd/aster-lang/blob/8dd861bbf5cdaedf2f100811eb1228c32bced27f/src/types.ts#L576)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/8dd861bbf5cdaedf2f100811eb1228c32bced27f/src/types/base.ts#L41)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`span`](../../../base/interfaces/BaseLambda.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/8dd861bbf5cdaedf2f100811eb1228c32bced27f/src/types/base.ts#L42)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`origin`](../../../base/interfaces/BaseLambda.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/8dd861bbf5cdaedf2f100811eb1228c32bced27f/src/types/base.ts#L43)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`file`](../../../base/interfaces/BaseLambda.md#file)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types/base.ts:365](https://github.com/wontlost-ltd/aster-lang/blob/8dd861bbf5cdaedf2f100811eb1228c32bced27f/src/types/base.ts#L365)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`kind`](../../../base/interfaces/BaseLambda.md#kind)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](../../../base/interfaces/BaseParameter.md)\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:366](https://github.com/wontlost-ltd/aster-lang/blob/8dd861bbf5cdaedf2f100811eb1228c32bced27f/src/types/base.ts#L366)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`params`](../../../base/interfaces/BaseLambda.md#params)

***

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:367](https://github.com/wontlost-ltd/aster-lang/blob/8dd861bbf5cdaedf2f100811eb1228c32bced27f/src/types/base.ts#L367)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`retType`](../../../base/interfaces/BaseLambda.md#rettype)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:368](https://github.com/wontlost-ltd/aster-lang/blob/8dd861bbf5cdaedf2f100811eb1228c32bced27f/src/types/base.ts#L368)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`body`](../../../base/interfaces/BaseLambda.md#body)
