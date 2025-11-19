[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: WorkflowStmt

Defined in: [types.ts:233](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types.ts#L233)

Workflow 语句基础接口。

## Extends

- [`BaseWorkflow`](../base/interfaces/BaseWorkflow.md)\<[`Span`](Span.md), [`StepStmt`](StepStmt.md), [`RetryPolicy`](RetryPolicy.md), [`Timeout`](Timeout.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:235](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types.ts#L235)

#### Overrides

[`BaseWorkflow`](../base/interfaces/BaseWorkflow.md).[`span`](../base/interfaces/BaseWorkflow.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L42)

#### Inherited from

[`BaseWorkflow`](../base/interfaces/BaseWorkflow.md).[`origin`](../base/interfaces/BaseWorkflow.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L43)

#### Inherited from

[`BaseWorkflow`](../base/interfaces/BaseWorkflow.md).[`file`](../base/interfaces/BaseWorkflow.md#file)

***

### kind

> `readonly` **kind**: `"workflow"`

Defined in: [types/base.ts:221](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L221)

#### Inherited from

[`Workflow`](../namespaces/Core/interfaces/Workflow.md).[`kind`](../namespaces/Core/interfaces/Workflow.md#kind)

***

### steps

> `readonly` **steps**: readonly [`StepStmt`](StepStmt.md)[]

Defined in: [types/base.ts:222](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L222)

#### Inherited from

[`BaseWorkflow`](../base/interfaces/BaseWorkflow.md).[`steps`](../base/interfaces/BaseWorkflow.md#steps)

***

### retry?

> `readonly` `optional` **retry**: [`RetryPolicy`](RetryPolicy.md)

Defined in: [types/base.ts:223](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L223)

#### Inherited from

[`BaseWorkflow`](../base/interfaces/BaseWorkflow.md).[`retry`](../base/interfaces/BaseWorkflow.md#retry-1)

***

### timeout?

> `readonly` `optional` **timeout**: [`Timeout`](Timeout.md)

Defined in: [types/base.ts:224](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L224)

#### Inherited from

[`BaseWorkflow`](../base/interfaces/BaseWorkflow.md).[`timeout`](../base/interfaces/BaseWorkflow.md#timeout-1)
