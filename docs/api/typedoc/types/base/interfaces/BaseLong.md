[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseLong\<S\>

Defined in: [types/base.ts:324](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/types/base.ts#L324)

Long 字面量基础接口。

**注意**: value 使用 string 类型存储以避免 JavaScript number 的精度损失。
Long 字面量可能超过 Number.MAX_SAFE_INTEGER (2^53-1)，例如 Long.MAX_VALUE (2^63-1)。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Long`](../../interfaces/Long.md)
- [`Long`](../../namespaces/Core/interfaces/Long.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Long"`

Defined in: [types/base.ts:325](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/types/base.ts#L325)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `string`

Defined in: [types/base.ts:326](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/types/base.ts#L326)
