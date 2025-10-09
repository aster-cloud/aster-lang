[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: FuncType

Defined in: [types.ts:365](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types.ts#L365)

函数类型基础接口。

## Extends

- [`BaseFuncType`](../../../base/interfaces/BaseFuncType.md)\<[`Origin`](../../../interfaces/Origin.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L32)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`span`](../../../base/interfaces/BaseFuncType.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L33)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`origin`](../../../base/interfaces/BaseFuncType.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L34)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`file`](../../../base/interfaces/BaseFuncType.md#file)

***

### kind

> `readonly` **kind**: `"FuncType"`

Defined in: [types/base.ts:448](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L448)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`kind`](../../../base/interfaces/BaseFuncType.md#kind)

***

### params

> `readonly` **params**: readonly [`Type`](../type-aliases/Type.md)[]

Defined in: [types/base.ts:449](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L449)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`params`](../../../base/interfaces/BaseFuncType.md#params)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:450](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L450)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`ret`](../../../base/interfaces/BaseFuncType.md#ret)
