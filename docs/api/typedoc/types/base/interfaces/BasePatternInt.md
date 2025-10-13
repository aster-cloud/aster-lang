[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BasePatternInt\<S\>

Defined in: [types/base.ts:235](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L235)

整数模式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`PatternInt`](../../interfaces/PatternInt.md)
- [`PatInt`](../../namespaces/Core/interfaces/PatInt.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"PatternInt"` \| `"PatInt"`

Defined in: [types/base.ts:236](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L236)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `number`

Defined in: [types/base.ts:237](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L237)
