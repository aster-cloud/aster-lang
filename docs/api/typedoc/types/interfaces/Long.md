[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Long

Defined in: [types.ts:270](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types.ts#L270)

Long 字面量基础接口。

**注意**: value 使用 string 类型存储以避免 JavaScript number 的精度损失。
Long 字面量可能超过 Number.MAX_SAFE_INTEGER (2^53-1)，例如 Long.MAX_VALUE (2^63-1)。

## Extends

- [`BaseLong`](../base/interfaces/BaseLong.md)\<[`Span`](Span.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:271](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types.ts#L271)

#### Overrides

[`BaseLong`](../base/interfaces/BaseLong.md).[`span`](../base/interfaces/BaseLong.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L42)

#### Inherited from

[`BaseLong`](../base/interfaces/BaseLong.md).[`origin`](../base/interfaces/BaseLong.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L43)

#### Inherited from

[`BaseLong`](../base/interfaces/BaseLong.md).[`file`](../base/interfaces/BaseLong.md#file)

***

### kind

> `readonly` **kind**: `"Long"`

Defined in: [types/base.ts:284](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L284)

#### Inherited from

[`Long`](../namespaces/Core/interfaces/Long.md).[`kind`](../namespaces/Core/interfaces/Long.md#kind)

***

### value

> `readonly` **value**: `string`

Defined in: [types/base.ts:285](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L285)

#### Inherited from

[`Long`](../namespaces/Core/interfaces/Long.md).[`value`](../namespaces/Core/interfaces/Long.md#value)
