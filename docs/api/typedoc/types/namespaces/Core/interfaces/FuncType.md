[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: FuncType

Defined in: [types.ts:539](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L539)

函数类型基础接口。

## Extends

- [`BaseFuncType`](../../../base/interfaces/BaseFuncType.md)\<[`Origin`](../../../interfaces/Origin.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L34)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`span`](../../../base/interfaces/BaseFuncType.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L35)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`origin`](../../../base/interfaces/BaseFuncType.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L36)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`file`](../../../base/interfaces/BaseFuncType.md#file)

***

### kind

> `readonly` **kind**: `"FuncType"`

Defined in: [types/base.ts:454](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L454)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`kind`](../../../base/interfaces/BaseFuncType.md#kind)

***

### params

> `readonly` **params**: readonly [`Type`](../type-aliases/Type.md)[]

Defined in: [types/base.ts:455](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L455)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`params`](../../../base/interfaces/BaseFuncType.md#params)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:456](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L456)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`ret`](../../../base/interfaces/BaseFuncType.md#ret)
