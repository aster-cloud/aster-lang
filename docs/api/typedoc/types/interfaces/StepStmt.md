[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: StepStmt

Defined in: [types.ts:237](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types.ts#L237)

Step 语句基础接口。

## Extends

- [`BaseStep`](../base/interfaces/BaseStep.md)\<[`Span`](Span.md), [`Block`](Block.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:238](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types.ts#L238)

#### Overrides

[`BaseStep`](../base/interfaces/BaseStep.md).[`span`](../base/interfaces/BaseStep.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L42)

#### Inherited from

[`BaseStep`](../base/interfaces/BaseStep.md).[`origin`](../base/interfaces/BaseStep.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L43)

#### Inherited from

[`BaseStep`](../base/interfaces/BaseStep.md).[`file`](../base/interfaces/BaseStep.md#file)

***

### kind

> `readonly` **kind**: `"step"`

Defined in: [types/base.ts:231](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L231)

#### Inherited from

[`Step`](../namespaces/Core/interfaces/Step.md).[`kind`](../namespaces/Core/interfaces/Step.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:232](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L232)

#### Inherited from

[`Step`](../namespaces/Core/interfaces/Step.md).[`name`](../namespaces/Core/interfaces/Step.md#name)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:233](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L233)

#### Inherited from

[`BaseStep`](../base/interfaces/BaseStep.md).[`body`](../base/interfaces/BaseStep.md#body)

***

### dependencies

> `readonly` **dependencies**: readonly `string`[]

Defined in: [types/base.ts:234](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L234)

#### Inherited from

[`Step`](../namespaces/Core/interfaces/Step.md).[`dependencies`](../namespaces/Core/interfaces/Step.md#dependencies)

***

### compensate?

> `readonly` `optional` **compensate**: [`Block`](Block.md)

Defined in: [types/base.ts:235](https://github.com/wontlost-ltd/aster-lang/blob/90c50dda0d3ed53f4a4a0435fba3a3f21e46c809/src/types/base.ts#L235)

#### Inherited from

[`BaseStep`](../base/interfaces/BaseStep.md).[`compensate`](../base/interfaces/BaseStep.md#compensate)
