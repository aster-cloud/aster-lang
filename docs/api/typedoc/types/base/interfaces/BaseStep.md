[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseStep\<S, Block\>

Defined in: [types/base.ts:230](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L230)

Step 语句基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`StepStmt`](../../interfaces/StepStmt.md)
- [`Step`](../../namespaces/Core/interfaces/Step.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Block

`Block` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"step"`

Defined in: [types/base.ts:231](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L231)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:232](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L232)

***

### body

> `readonly` **body**: `Block`

Defined in: [types/base.ts:233](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L233)

***

### dependencies

> `readonly` **dependencies**: readonly `string`[]

Defined in: [types/base.ts:234](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L234)

***

### compensate?

> `readonly` `optional` **compensate**: `Block`

Defined in: [types/base.ts:235](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L235)
