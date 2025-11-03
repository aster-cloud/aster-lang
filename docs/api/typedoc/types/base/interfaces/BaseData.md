[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseData\<S, T\>

Defined in: [types/base.ts:62](https://github.com/wontlost-ltd/aster-lang/blob/0b0406c33523b608f58b6400b072157cb0880387/src/types/base.ts#L62)

Data 类型声明基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Data`](../../interfaces/Data.md)
- [`Data`](../../namespaces/Core/interfaces/Data.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

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

> `readonly` **kind**: `"Data"`

Defined in: [types/base.ts:63](https://github.com/wontlost-ltd/aster-lang/blob/0b0406c33523b608f58b6400b072157cb0880387/src/types/base.ts#L63)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:64](https://github.com/wontlost-ltd/aster-lang/blob/0b0406c33523b608f58b6400b072157cb0880387/src/types/base.ts#L64)

***

### fields

> `readonly` **fields**: readonly [`BaseField`](BaseField.md)\<`T`\>[]

Defined in: [types/base.ts:65](https://github.com/wontlost-ltd/aster-lang/blob/0b0406c33523b608f58b6400b072157cb0880387/src/types/base.ts#L65)
