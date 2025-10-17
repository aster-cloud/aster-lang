[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseData\<S, T\>

Defined in: [types/base.ts:53](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L53)

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

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Data"`

Defined in: [types/base.ts:54](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L54)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:55](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L55)

***

### fields

> `readonly` **fields**: readonly [`BaseField`](BaseField.md)\<`T`\>[]

Defined in: [types/base.ts:56](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L56)
