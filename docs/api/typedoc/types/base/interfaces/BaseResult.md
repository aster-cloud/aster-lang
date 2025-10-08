[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseResult\<S, T\>

Defined in: [types/base.ts:421](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L421)

Result 类型基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Result`](../../interfaces/Result.md)
- [`Result`](../../namespaces/Core/interfaces/Result.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

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

> `readonly` **kind**: `"Result"`

Defined in: [types/base.ts:422](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L422)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### ok

> `readonly` **ok**: `T`

Defined in: [types/base.ts:423](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L423)

***

### err

> `readonly` **err**: `T`

Defined in: [types/base.ts:424](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L424)
