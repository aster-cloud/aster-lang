[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseImport\<S\>

Defined in: [types/base.ts:44](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types/base.ts#L44)

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

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Import"`

Defined in: [types/base.ts:45](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types/base.ts#L45)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:46](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types/base.ts#L46)

***

### asName

> `readonly` **asName**: `null` \| `string`

Defined in: [types/base.ts:47](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types/base.ts#L47)
