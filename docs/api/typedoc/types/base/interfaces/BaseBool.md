[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseBool\<S\>

Defined in: [types/base.ts:264](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L264)

布尔字面量基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Bool`](../../interfaces/Bool.md)
- [`Bool`](../../namespaces/Core/interfaces/Bool.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Bool"`

Defined in: [types/base.ts:265](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L265)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `boolean`

Defined in: [types/base.ts:266](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L266)
