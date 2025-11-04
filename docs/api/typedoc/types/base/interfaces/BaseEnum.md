[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseEnum\<S\>

Defined in: [types/base.ts:81](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L81)

Enum 声明基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Enum`](../../interfaces/Enum.md)
- [`Enum`](../../namespaces/Core/interfaces/Enum.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Enum"`

Defined in: [types/base.ts:82](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L82)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:83](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L83)

***

### variants

> `readonly` **variants**: readonly `string`[]

Defined in: [types/base.ts:84](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L84)
