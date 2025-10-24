[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Long

Defined in: [types.ts:269](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L269)

Long 字面量基础接口。

**注意**: value 使用 string 类型存储以避免 JavaScript number 的精度损失。
Long 字面量可能超过 Number.MAX_SAFE_INTEGER (2^53-1)，例如 Long.MAX_VALUE (2^63-1)。

## Extends

- [`BaseLong`](../base/interfaces/BaseLong.md)\<[`Span`](Span.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:270](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L270)

#### Overrides

[`BaseLong`](../base/interfaces/BaseLong.md).[`span`](../base/interfaces/BaseLong.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L35)

#### Inherited from

[`BaseLong`](../base/interfaces/BaseLong.md).[`origin`](../base/interfaces/BaseLong.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L36)

#### Inherited from

[`BaseLong`](../base/interfaces/BaseLong.md).[`file`](../base/interfaces/BaseLong.md#file)

***

### kind

> `readonly` **kind**: `"Long"`

Defined in: [types/base.ts:277](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L277)

#### Inherited from

[`Long`](../namespaces/Core/interfaces/Long.md).[`kind`](../namespaces/Core/interfaces/Long.md#kind)

***

### value

> `readonly` **value**: `string`

Defined in: [types/base.ts:278](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L278)

#### Inherited from

[`Long`](../namespaces/Core/interfaces/Long.md).[`value`](../namespaces/Core/interfaces/Long.md#value)
