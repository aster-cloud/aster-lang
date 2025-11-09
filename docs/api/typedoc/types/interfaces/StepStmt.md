[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: StepStmt

Defined in: [types.ts:237](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types.ts#L237)

Step 语句基础接口。

## Extends

- [`BaseStep`](../base/interfaces/BaseStep.md)\<[`Span`](Span.md), [`Block`](Block.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:238](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types.ts#L238)

#### Overrides

[`BaseStep`](../base/interfaces/BaseStep.md).[`span`](../base/interfaces/BaseStep.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L42)

#### Inherited from

[`BaseStep`](../base/interfaces/BaseStep.md).[`origin`](../base/interfaces/BaseStep.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L43)

#### Inherited from

[`BaseStep`](../base/interfaces/BaseStep.md).[`file`](../base/interfaces/BaseStep.md#file)

***

### kind

> `readonly` **kind**: `"step"`

Defined in: [types/base.ts:231](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L231)

#### Inherited from

[`Step`](../namespaces/Core/interfaces/Step.md).[`kind`](../namespaces/Core/interfaces/Step.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:232](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L232)

#### Inherited from

[`Step`](../namespaces/Core/interfaces/Step.md).[`name`](../namespaces/Core/interfaces/Step.md#name)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:233](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L233)

#### Inherited from

[`BaseStep`](../base/interfaces/BaseStep.md).[`body`](../base/interfaces/BaseStep.md#body)

***

### compensate?

> `readonly` `optional` **compensate**: [`Block`](Block.md)

Defined in: [types/base.ts:234](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L234)

#### Inherited from

[`BaseStep`](../base/interfaces/BaseStep.md).[`compensate`](../base/interfaces/BaseStep.md#compensate)
