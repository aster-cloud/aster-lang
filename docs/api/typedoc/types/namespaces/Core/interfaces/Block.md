[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Block

Defined in: [types.ts:451](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types.ts#L451)

Block 基础接口。

## Extends

- [`BaseBlock`](../../../base/interfaces/BaseBlock.md)\<[`Origin`](../../../interfaces/Origin.md), [`Statement`](../type-aliases/Statement.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L41)

#### Inherited from

[`BaseBlock`](../../../base/interfaces/BaseBlock.md).[`span`](../../../base/interfaces/BaseBlock.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L42)

#### Inherited from

[`BaseBlock`](../../../base/interfaces/BaseBlock.md).[`origin`](../../../base/interfaces/BaseBlock.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L43)

#### Inherited from

[`BaseBlock`](../../../base/interfaces/BaseBlock.md).[`file`](../../../base/interfaces/BaseBlock.md#file)

***

### kind

> `readonly` **kind**: `"Block"`

Defined in: [types/base.ts:155](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L155)

#### Inherited from

[`BaseBlock`](../../../base/interfaces/BaseBlock.md).[`kind`](../../../base/interfaces/BaseBlock.md#kind)

***

### statements

> `readonly` **statements**: readonly [`Statement`](../type-aliases/Statement.md)[]

Defined in: [types/base.ts:156](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L156)

#### Inherited from

[`BaseBlock`](../../../base/interfaces/BaseBlock.md).[`statements`](../../../base/interfaces/BaseBlock.md#statements)
