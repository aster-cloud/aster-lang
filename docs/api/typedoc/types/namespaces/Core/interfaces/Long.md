[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Long

Defined in: [types.ts:564](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types.ts#L564)

Long 字面量基础接口。

**注意**: value 使用 string 类型存储以避免 JavaScript number 的精度损失。
Long 字面量可能超过 Number.MAX_SAFE_INTEGER (2^53-1)，例如 Long.MAX_VALUE (2^63-1)。

## Extends

- [`BaseLong`](../../../base/interfaces/BaseLong.md)\<[`Origin`](../../../interfaces/Origin.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types/base.ts#L41)

#### Inherited from

[`BaseLong`](../../../base/interfaces/BaseLong.md).[`span`](../../../base/interfaces/BaseLong.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types/base.ts#L42)

#### Inherited from

[`BaseLong`](../../../base/interfaces/BaseLong.md).[`origin`](../../../base/interfaces/BaseLong.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types/base.ts#L43)

#### Inherited from

[`BaseLong`](../../../base/interfaces/BaseLong.md).[`file`](../../../base/interfaces/BaseLong.md#file)

***

### kind

> `readonly` **kind**: `"Long"`

Defined in: [types/base.ts:325](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types/base.ts#L325)

#### Inherited from

[`BaseLong`](../../../base/interfaces/BaseLong.md).[`kind`](../../../base/interfaces/BaseLong.md#kind)

***

### value

> `readonly` **value**: `string`

Defined in: [types/base.ts:326](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types/base.ts#L326)

#### Inherited from

[`BaseLong`](../../../base/interfaces/BaseLong.md).[`value`](../../../base/interfaces/BaseLong.md#value)
