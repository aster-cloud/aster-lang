[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Call

Defined in: [types.ts:490](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types.ts#L490)

函数调用基础接口。

## Extends

- [`BaseCall`](../../../base/interfaces/BaseCall.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L41)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`span`](../../../base/interfaces/BaseCall.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L42)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`origin`](../../../base/interfaces/BaseCall.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L43)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`file`](../../../base/interfaces/BaseCall.md#file)

***

### kind

> `readonly` **kind**: `"Call"`

Defined in: [types/base.ts:315](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L315)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`kind`](../../../base/interfaces/BaseCall.md#kind)

***

### target

> `readonly` **target**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:316](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L316)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`target`](../../../base/interfaces/BaseCall.md#target)

***

### args

> `readonly` **args**: readonly [`Expression`](../type-aliases/Expression.md)[]

Defined in: [types/base.ts:317](https://github.com/wontlost-ltd/aster-lang/blob/58bfaa371c511ea13bda82442d3d29ba7681c072/src/types/base.ts#L317)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`args`](../../../base/interfaces/BaseCall.md#args)
