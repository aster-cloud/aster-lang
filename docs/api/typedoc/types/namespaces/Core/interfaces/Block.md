[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Block

Defined in: [types.ts:423](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L423)

Block 基础接口。

## Extends

- [`BaseBlock`](../../../base/interfaces/BaseBlock.md)\<[`Origin`](../../../interfaces/Origin.md), [`Statement`](../type-aliases/Statement.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L34)

#### Inherited from

[`BaseBlock`](../../../base/interfaces/BaseBlock.md).[`span`](../../../base/interfaces/BaseBlock.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L35)

#### Inherited from

[`BaseBlock`](../../../base/interfaces/BaseBlock.md).[`origin`](../../../base/interfaces/BaseBlock.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L36)

#### Inherited from

[`BaseBlock`](../../../base/interfaces/BaseBlock.md).[`file`](../../../base/interfaces/BaseBlock.md#file)

***

### kind

> `readonly` **kind**: `"Block"`

Defined in: [types/base.ts:148](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L148)

#### Inherited from

[`BaseBlock`](../../../base/interfaces/BaseBlock.md).[`kind`](../../../base/interfaces/BaseBlock.md#kind)

***

### statements

> `readonly` **statements**: readonly [`Statement`](../type-aliases/Statement.md)[]

Defined in: [types/base.ts:149](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L149)

#### Inherited from

[`BaseBlock`](../../../base/interfaces/BaseBlock.md).[`statements`](../../../base/interfaces/BaseBlock.md#statements)
