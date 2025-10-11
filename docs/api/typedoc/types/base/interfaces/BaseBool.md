[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseBool\<S\>

Defined in: [types/base.ts:255](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types/base.ts#L255)

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

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Bool"`

Defined in: [types/base.ts:256](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types/base.ts#L256)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `boolean`

Defined in: [types/base.ts:257](https://github.com/wontlost-ltd/aster-lang/blob/af4125774ad182976762e13ef712d3d624eaba26/src/types/base.ts#L257)
