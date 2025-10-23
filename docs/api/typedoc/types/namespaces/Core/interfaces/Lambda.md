[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:490](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types.ts#L490)

Lambda 表达式基础接口。

## Extends

- [`BaseLambda`](../../../base/interfaces/BaseLambda.md)\<[`Origin`](../../../interfaces/Origin.md), [`Type`](../type-aliases/Type.md), [`Block`](Block.md)\>

## Properties

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:491](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types.ts#L491)

***

### captures?

> `readonly` `optional` **captures**: readonly `string`[]

Defined in: [types.ts:492](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types.ts#L492)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L34)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`span`](../../../base/interfaces/BaseLambda.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L35)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`origin`](../../../base/interfaces/BaseLambda.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L36)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`file`](../../../base/interfaces/BaseLambda.md#file)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types/base.ts:317](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L317)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`kind`](../../../base/interfaces/BaseLambda.md#kind)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](../../../base/interfaces/BaseParameter.md)\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:318](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L318)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`params`](../../../base/interfaces/BaseLambda.md#params)

***

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:319](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L319)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`retType`](../../../base/interfaces/BaseLambda.md#rettype)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:320](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L320)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`body`](../../../base/interfaces/BaseLambda.md#body)
