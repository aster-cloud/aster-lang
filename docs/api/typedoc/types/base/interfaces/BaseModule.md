[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseModule\<S, D\>

Defined in: [types/base.ts:90](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L90)

Module 模块基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Module`](../../interfaces/Module.md)
- [`Module`](../../namespaces/Core/interfaces/Module.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### D

`D` = `unknown`

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

> `readonly` **kind**: `"Module"`

Defined in: [types/base.ts:91](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L91)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `null` \| `string`

Defined in: [types/base.ts:92](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L92)

***

### decls

> `readonly` **decls**: readonly `D`[]

Defined in: [types/base.ts:93](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L93)
