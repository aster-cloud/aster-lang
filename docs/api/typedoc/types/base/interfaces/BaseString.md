[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseString\<S\>

Defined in: [types/base.ts:287](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L287)

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

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"String"`

Defined in: [types/base.ts:288](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L288)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `string`

Defined in: [types/base.ts:289](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L289)
