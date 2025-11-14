[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseString\<S\>

Defined in: [types/base.ts:340](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L340)

字符串字面量基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`String`](../../interfaces/String.md)
- [`String`](../../namespaces/Core/interfaces/String.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"String"`

Defined in: [types/base.ts:341](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L341)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `string`

Defined in: [types/base.ts:342](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L342)
