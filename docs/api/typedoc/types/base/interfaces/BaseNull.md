[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseNull\<S\>

Defined in: [types/base.ts:294](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L294)

Null 字面量基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Null`](../../interfaces/Null.md)
- [`Null`](../../namespaces/Core/interfaces/Null.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Null"`

Defined in: [types/base.ts:295](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L295)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)
