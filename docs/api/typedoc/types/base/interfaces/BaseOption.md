[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseOption\<S, T\>

Defined in: [types/base.ts:414](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L414)

Option 类型基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Option`](../../interfaces/Option.md)
- [`Option`](../../namespaces/Core/interfaces/Option.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

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

> `readonly` **kind**: `"Option"`

Defined in: [types/base.ts:415](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L415)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### type

> `readonly` **type**: `T`

Defined in: [types/base.ts:416](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L416)
