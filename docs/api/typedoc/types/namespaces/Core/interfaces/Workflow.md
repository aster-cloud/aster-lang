[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Workflow

Defined in: [types.ts:461](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types.ts#L461)

Workflow 语句基础接口。

## Extends

- [`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md)\<[`Origin`](../../../interfaces/Origin.md), [`Step`](Step.md), [`RetryPolicy`](RetryPolicy.md), [`Timeout`](Timeout.md)\>

## Properties

### effectCaps

> `readonly` **effectCaps**: [`EffectCaps`](../../../type-aliases/EffectCaps.md)

Defined in: [types.ts:463](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types.ts#L463)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L41)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`span`](../../../base/interfaces/BaseWorkflow.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L42)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`origin`](../../../base/interfaces/BaseWorkflow.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L43)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`file`](../../../base/interfaces/BaseWorkflow.md#file)

***

### kind

> `readonly` **kind**: `"workflow"`

Defined in: [types/base.ts:221](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L221)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`kind`](../../../base/interfaces/BaseWorkflow.md#kind)

***

### steps

> `readonly` **steps**: readonly [`Step`](Step.md)[]

Defined in: [types/base.ts:222](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L222)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`steps`](../../../base/interfaces/BaseWorkflow.md#steps)

***

### retry?

> `readonly` `optional` **retry**: [`RetryPolicy`](RetryPolicy.md)

Defined in: [types/base.ts:223](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L223)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`retry`](../../../base/interfaces/BaseWorkflow.md#retry-1)

***

### timeout?

> `readonly` `optional` **timeout**: [`Timeout`](Timeout.md)

Defined in: [types/base.ts:224](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L224)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`timeout`](../../../base/interfaces/BaseWorkflow.md#timeout-1)
