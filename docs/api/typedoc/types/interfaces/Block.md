[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Block

Defined in: [types.ts:183](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types.ts#L183)

Block 基础接口。

## Extends

- [`BaseBlock`](../base/interfaces/BaseBlock.md)\<[`Span`](Span.md), [`Statement`](../type-aliases/Statement.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:184](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types.ts#L184)

#### Overrides

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`span`](../base/interfaces/BaseBlock.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types/base.ts#L42)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`origin`](../base/interfaces/BaseBlock.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types/base.ts#L43)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`file`](../base/interfaces/BaseBlock.md#file)

***

### kind

> `readonly` **kind**: `"Block"`

Defined in: [types/base.ts:155](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types/base.ts#L155)

#### Inherited from

[`Block`](../namespaces/Core/interfaces/Block.md).[`kind`](../namespaces/Core/interfaces/Block.md#kind)

***

### statements

> `readonly` **statements**: readonly [`Statement`](../type-aliases/Statement.md)[]

Defined in: [types/base.ts:156](https://github.com/wontlost-ltd/aster-lang/blob/86a42486bd55428e1a25b42c1154fd2f922f9f45/src/types/base.ts#L156)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`statements`](../base/interfaces/BaseBlock.md#statements)
