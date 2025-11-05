[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Result

Defined in: [types.ts:535](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types.ts#L535)

Result 类型基础接口。

## Extends

- [`BaseResult`](../../../base/interfaces/BaseResult.md)\<[`Origin`](../../../interfaces/Origin.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L41)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`span`](../../../base/interfaces/BaseResult.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L42)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`origin`](../../../base/interfaces/BaseResult.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L43)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`file`](../../../base/interfaces/BaseResult.md#file)

***

### kind

> `readonly` **kind**: `"Result"`

Defined in: [types/base.ts:435](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L435)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`kind`](../../../base/interfaces/BaseResult.md#kind)

***

### ok

> `readonly` **ok**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:436](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L436)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`ok`](../../../base/interfaces/BaseResult.md#ok)

***

### err

> `readonly` **err**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:437](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L437)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`err`](../../../base/interfaces/BaseResult.md#err)
