[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BasePatternNull\<S\>

Defined in: [types/base.ts:209](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L209)

Null 模式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`PatternNull`](../../interfaces/PatternNull.md)
- [`PatNull`](../../namespaces/Core/interfaces/PatNull.md)

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

> `readonly` **kind**: `"PatternNull"` \| `"PatNull"`

Defined in: [types/base.ts:210](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L210)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)
