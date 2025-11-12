[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseBlock\<S, Stmt\>

Defined in: [types/base.ts:154](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L154)

Block 基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Block`](../../interfaces/Block.md)
- [`Block`](../../namespaces/Core/interfaces/Block.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Stmt

`Stmt` = `unknown`

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

> `readonly` **kind**: `"Block"`

Defined in: [types/base.ts:155](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L155)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### statements

> `readonly` **statements**: readonly `Stmt`[]

Defined in: [types/base.ts:156](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L156)
