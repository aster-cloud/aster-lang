[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: FuncType

Defined in: [types.ts:638](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types.ts#L638)

函数类型基础接口。

## Extends

- [`BaseFuncType`](../../../base/interfaces/BaseFuncType.md)\<[`Origin`](../../../interfaces/Origin.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### effectParams?

> `readonly` `optional` **effectParams**: readonly `string`[]

Defined in: [types.ts:639](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types.ts#L639)

***

### declaredEffects?

> `readonly` `optional` **declaredEffects**: readonly `string`[]

Defined in: [types.ts:640](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types.ts#L640)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L41)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`span`](../../../base/interfaces/BaseFuncType.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L42)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`origin`](../../../base/interfaces/BaseFuncType.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L43)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`file`](../../../base/interfaces/BaseFuncType.md#file)

***

### kind

> `readonly` **kind**: `"FuncType"`

Defined in: [types/base.ts:502](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L502)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`kind`](../../../base/interfaces/BaseFuncType.md#kind)

***

### params

> `readonly` **params**: readonly [`Type`](../type-aliases/Type.md)[]

Defined in: [types/base.ts:503](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L503)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`params`](../../../base/interfaces/BaseFuncType.md#params)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:504](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L504)

#### Inherited from

[`BaseFuncType`](../../../base/interfaces/BaseFuncType.md).[`ret`](../../../base/interfaces/BaseFuncType.md#ret)
