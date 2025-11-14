[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseInt\<S\>

Defined in: [types/base.ts:313](https://github.com/wontlost-ltd/aster-lang/blob/d0422831ed603278d294a40664f5f8949bd9d592/src/types/base.ts#L313)

整数字面量基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Int`](../../interfaces/Int.md)
- [`Int`](../../namespaces/Core/interfaces/Int.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/d0422831ed603278d294a40664f5f8949bd9d592/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/d0422831ed603278d294a40664f5f8949bd9d592/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/d0422831ed603278d294a40664f5f8949bd9d592/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Int"`

Defined in: [types/base.ts:314](https://github.com/wontlost-ltd/aster-lang/blob/d0422831ed603278d294a40664f5f8949bd9d592/src/types/base.ts#L314)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `number`

Defined in: [types/base.ts:315](https://github.com/wontlost-ltd/aster-lang/blob/d0422831ed603278d294a40664f5f8949bd9d592/src/types/base.ts#L315)
