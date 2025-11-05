[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Block

Defined in: [types.ts:178](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types.ts#L178)

Block 基础接口。

## Extends

- [`BaseBlock`](../base/interfaces/BaseBlock.md)\<[`Span`](Span.md), [`Statement`](../type-aliases/Statement.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:179](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types.ts#L179)

#### Overrides

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`span`](../base/interfaces/BaseBlock.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L42)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`origin`](../base/interfaces/BaseBlock.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L43)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`file`](../base/interfaces/BaseBlock.md#file)

***

### kind

> `readonly` **kind**: `"Block"`

Defined in: [types/base.ts:155](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L155)

#### Inherited from

[`Block`](../namespaces/Core/interfaces/Block.md).[`kind`](../namespaces/Core/interfaces/Block.md#kind)

***

### statements

> `readonly` **statements**: readonly [`Statement`](../type-aliases/Statement.md)[]

Defined in: [types/base.ts:156](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L156)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`statements`](../base/interfaces/BaseBlock.md#statements)
