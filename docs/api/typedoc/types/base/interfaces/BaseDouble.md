[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseDouble\<S\>

Defined in: [types/base.ts:291](https://github.com/wontlost-ltd/aster-lang/blob/0b0406c33523b608f58b6400b072157cb0880387/src/types/base.ts#L291)

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

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/0b0406c33523b608f58b6400b072157cb0880387/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/0b0406c33523b608f58b6400b072157cb0880387/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/0b0406c33523b608f58b6400b072157cb0880387/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Double"`

Defined in: [types/base.ts:292](https://github.com/wontlost-ltd/aster-lang/blob/0b0406c33523b608f58b6400b072157cb0880387/src/types/base.ts#L292)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `number`

Defined in: [types/base.ts:293](https://github.com/wontlost-ltd/aster-lang/blob/0b0406c33523b608f58b6400b072157cb0880387/src/types/base.ts#L293)
