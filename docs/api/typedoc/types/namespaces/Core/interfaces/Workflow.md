[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Workflow

Defined in: [types.ts:472](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types.ts#L472)

Workflow 语句基础接口。

## Extends

- [`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md)\<[`Origin`](../../../interfaces/Origin.md), [`Step`](Step.md), [`RetryPolicy`](RetryPolicy.md), [`Timeout`](Timeout.md)\>

## Properties

### effectCaps

> `readonly` **effectCaps**: [`EffectCaps`](../../../type-aliases/EffectCaps.md)

Defined in: [types.ts:474](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types.ts#L474)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L41)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`span`](../../../base/interfaces/BaseWorkflow.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L42)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`origin`](../../../base/interfaces/BaseWorkflow.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L43)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`file`](../../../base/interfaces/BaseWorkflow.md#file)

***

### kind

> `readonly` **kind**: `"workflow"`

Defined in: [types/base.ts:221](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L221)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`kind`](../../../base/interfaces/BaseWorkflow.md#kind)

***

### steps

> `readonly` **steps**: readonly [`Step`](Step.md)[]

Defined in: [types/base.ts:222](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L222)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`steps`](../../../base/interfaces/BaseWorkflow.md#steps)

***

### retry?

> `readonly` `optional` **retry**: [`RetryPolicy`](RetryPolicy.md)

Defined in: [types/base.ts:223](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L223)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`retry`](../../../base/interfaces/BaseWorkflow.md#retry-1)

***

### timeout?

> `readonly` `optional` **timeout**: [`Timeout`](Timeout.md)

Defined in: [types/base.ts:224](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L224)

#### Inherited from

[`BaseWorkflow`](../../../base/interfaces/BaseWorkflow.md).[`timeout`](../../../base/interfaces/BaseWorkflow.md#timeout-1)
