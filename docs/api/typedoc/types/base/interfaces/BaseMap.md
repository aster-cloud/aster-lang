[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseMap\<S, T\>

Defined in: [types/base.ts:439](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types/base.ts#L439)

Map 类型基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Map`](../../interfaces/Map.md)
- [`Map`](../../namespaces/Core/interfaces/Map.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Map"`

Defined in: [types/base.ts:440](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types/base.ts#L440)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### key

> `readonly` **key**: `T`

Defined in: [types/base.ts:441](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types/base.ts#L441)

***

### val

> `readonly` **val**: `T`

Defined in: [types/base.ts:442](https://github.com/wontlost-ltd/aster-lang/blob/273a9355e5cfd44b5d3dd2c5d7f7bedf91cad0e5/src/types/base.ts#L442)
