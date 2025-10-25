[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseTypeName\<S\>

Defined in: [types/base.ts:386](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L386)

类型名称基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`TypeName`](../../interfaces/TypeName.md)
- [`TypeName`](../../namespaces/Core/interfaces/TypeName.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: `HasFileProp`\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `HasFileProp`\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L35)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L36)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"TypeName"`

Defined in: [types/base.ts:387](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L387)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:388](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L388)
