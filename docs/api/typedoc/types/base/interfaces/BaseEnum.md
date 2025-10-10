[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseEnum\<S\>

Defined in: [types/base.ts:72](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types/base.ts#L72)

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

> `readonly` **kind**: `"Enum"`

Defined in: [types/base.ts:73](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types/base.ts#L73)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:74](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types/base.ts#L74)

***

### variants

> `readonly` **variants**: readonly `string`[]

Defined in: [types/base.ts:75](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types/base.ts#L75)
