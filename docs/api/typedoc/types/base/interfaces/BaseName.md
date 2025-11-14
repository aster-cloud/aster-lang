[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseName\<S\>

Defined in: [types/base.ts:297](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L297)

名称表达式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Name`](../../interfaces/Name.md)
- [`Name`](../../namespaces/Core/interfaces/Name.md)

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

> `readonly` **kind**: `"Name"`

Defined in: [types/base.ts:298](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L298)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:299](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L299)
