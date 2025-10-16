[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseMaybe\<S, T\>

Defined in: [types/base.ts:406](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L406)

Maybe 类型基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Maybe`](../../interfaces/Maybe.md)
- [`Maybe`](../../namespaces/Core/interfaces/Maybe.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Maybe"`

Defined in: [types/base.ts:407](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L407)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### type

> `readonly` **type**: `T`

Defined in: [types/base.ts:408](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L408)
