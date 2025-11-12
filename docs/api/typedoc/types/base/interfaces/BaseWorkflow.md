[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseWorkflow\<S, StepStmt, Retry, Timeout\>

Defined in: [types/base.ts:215](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L215)

Workflow 语句基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`WorkflowStmt`](../../interfaces/WorkflowStmt.md)
- [`Workflow`](../../namespaces/Core/interfaces/Workflow.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### StepStmt

`StepStmt` = `unknown`

### Retry

`Retry` = `unknown`

### Timeout

`Timeout` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"workflow"`

Defined in: [types/base.ts:221](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L221)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### steps

> `readonly` **steps**: readonly `StepStmt`[]

Defined in: [types/base.ts:222](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L222)

***

### retry?

> `readonly` `optional` **retry**: `Retry`

Defined in: [types/base.ts:223](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L223)

***

### timeout?

> `readonly` `optional` **timeout**: `Timeout`

Defined in: [types/base.ts:224](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L224)
