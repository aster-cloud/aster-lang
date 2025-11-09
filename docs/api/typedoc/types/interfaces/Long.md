[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Long

Defined in: [types.ts:296](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types.ts#L296)

Long 字面量基础接口。

**注意**: value 使用 string 类型存储以避免 JavaScript number 的精度损失。
Long 字面量可能超过 Number.MAX_SAFE_INTEGER (2^53-1)，例如 Long.MAX_VALUE (2^63-1)。

## Extends

- [`BaseLong`](../base/interfaces/BaseLong.md)\<[`Span`](Span.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:297](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types.ts#L297)

#### Overrides

[`BaseLong`](../base/interfaces/BaseLong.md).[`span`](../base/interfaces/BaseLong.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L42)

#### Inherited from

[`BaseLong`](../base/interfaces/BaseLong.md).[`origin`](../base/interfaces/BaseLong.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L43)

#### Inherited from

[`BaseLong`](../base/interfaces/BaseLong.md).[`file`](../base/interfaces/BaseLong.md#file)

***

### kind

> `readonly` **kind**: `"Long"`

Defined in: [types/base.ts:324](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L324)

#### Inherited from

[`Long`](../namespaces/Core/interfaces/Long.md).[`kind`](../namespaces/Core/interfaces/Long.md#kind)

***

### value

> `readonly` **value**: `string`

Defined in: [types/base.ts:325](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L325)

#### Inherited from

[`Long`](../namespaces/Core/interfaces/Long.md).[`value`](../namespaces/Core/interfaces/Long.md#value)
