[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseImport\<S\>

Defined in: [types/base.ts:53](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L53)

Import 声明基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Import`](../../interfaces/Import.md)
- [`Import`](../../namespaces/Core/interfaces/Import.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Import"`

Defined in: [types/base.ts:54](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L54)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:55](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L55)

***

### asName

> `readonly` **asName**: `string` \| `null`

Defined in: [types/base.ts:56](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L56)
