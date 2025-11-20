[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseDouble\<S\>

Defined in: [types/base.ts:332](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L332)

浮点数字面量基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Double`](../../interfaces/Double.md)
- [`Double`](../../namespaces/Core/interfaces/Double.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Double"`

Defined in: [types/base.ts:333](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L333)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `number`

Defined in: [types/base.ts:334](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L334)
