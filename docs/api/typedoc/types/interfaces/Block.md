[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Block

Defined in: [types.ts:160](https://github.com/wontlost-ltd/aster-lang/blob/a9e060fb41d6a294aa63fe17f1f1a4eebbf9d535/src/types.ts#L160)

Block 基础接口。

## Extends

- [`BaseBlock`](../base/interfaces/BaseBlock.md)\<[`Span`](Span.md), [`Statement`](../type-aliases/Statement.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/a9e060fb41d6a294aa63fe17f1f1a4eebbf9d535/src/types/base.ts#L32)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`span`](../base/interfaces/BaseBlock.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/a9e060fb41d6a294aa63fe17f1f1a4eebbf9d535/src/types/base.ts#L33)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`origin`](../base/interfaces/BaseBlock.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/a9e060fb41d6a294aa63fe17f1f1a4eebbf9d535/src/types/base.ts#L34)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`file`](../base/interfaces/BaseBlock.md#file)

***

### kind

> `readonly` **kind**: `"Block"`

Defined in: [types/base.ts:146](https://github.com/wontlost-ltd/aster-lang/blob/a9e060fb41d6a294aa63fe17f1f1a4eebbf9d535/src/types/base.ts#L146)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`kind`](../base/interfaces/BaseBlock.md#kind)

***

### statements

> `readonly` **statements**: readonly [`Statement`](../type-aliases/Statement.md)[]

Defined in: [types/base.ts:147](https://github.com/wontlost-ltd/aster-lang/blob/a9e060fb41d6a294aa63fe17f1f1a4eebbf9d535/src/types/base.ts#L147)

#### Inherited from

[`BaseBlock`](../base/interfaces/BaseBlock.md).[`statements`](../base/interfaces/BaseBlock.md#statements)
