[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Step

Defined in: [types.ts:500](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types.ts#L500)

Step 语句基础接口。

## Extends

- [`BaseStep`](../../../base/interfaces/BaseStep.md)\<[`Origin`](../../../interfaces/Origin.md), [`Block`](Block.md)\>

## Properties

### effectCaps

> `readonly` **effectCaps**: [`EffectCaps`](../../../type-aliases/EffectCaps.md)

Defined in: [types.ts:501](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types.ts#L501)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L41)

#### Inherited from

[`BaseStep`](../../../base/interfaces/BaseStep.md).[`span`](../../../base/interfaces/BaseStep.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L42)

#### Inherited from

[`BaseStep`](../../../base/interfaces/BaseStep.md).[`origin`](../../../base/interfaces/BaseStep.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L43)

#### Inherited from

[`BaseStep`](../../../base/interfaces/BaseStep.md).[`file`](../../../base/interfaces/BaseStep.md#file)

***

### kind

> `readonly` **kind**: `"step"`

Defined in: [types/base.ts:231](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L231)

#### Inherited from

[`BaseStep`](../../../base/interfaces/BaseStep.md).[`kind`](../../../base/interfaces/BaseStep.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:232](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L232)

#### Inherited from

[`BaseStep`](../../../base/interfaces/BaseStep.md).[`name`](../../../base/interfaces/BaseStep.md#name)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:233](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L233)

#### Inherited from

[`BaseStep`](../../../base/interfaces/BaseStep.md).[`body`](../../../base/interfaces/BaseStep.md#body)

***

### dependencies

> `readonly` **dependencies**: readonly `string`[]

Defined in: [types/base.ts:234](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L234)

#### Inherited from

[`BaseStep`](../../../base/interfaces/BaseStep.md).[`dependencies`](../../../base/interfaces/BaseStep.md#dependencies)

***

### compensate?

> `readonly` `optional` **compensate**: [`Block`](Block.md)

Defined in: [types/base.ts:235](https://github.com/wontlost-ltd/aster-lang/blob/fb3b3f0e30c48d75f7f12ac3160d2dcc50a7e6cc/src/types/base.ts#L235)

#### Inherited from

[`BaseStep`](../../../base/interfaces/BaseStep.md).[`compensate`](../../../base/interfaces/BaseStep.md#compensate)
