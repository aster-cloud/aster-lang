[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseMap\<S, T\>

Defined in: [types/base.ts:451](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L451)

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

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Map"`

Defined in: [types/base.ts:452](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L452)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### key

> `readonly` **key**: `T`

Defined in: [types/base.ts:453](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L453)

***

### val

> `readonly` **val**: `T`

Defined in: [types/base.ts:454](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L454)
