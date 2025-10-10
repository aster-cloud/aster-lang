[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseTypeName\<S\>

Defined in: [types/base.ts:381](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types/base.ts#L381)

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

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"TypeName"`

Defined in: [types/base.ts:382](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types/base.ts#L382)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:383](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types/base.ts#L383)
