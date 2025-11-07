[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BasePatternInt\<S\>

Defined in: [types/base.ts:244](https://github.com/wontlost-ltd/aster-lang/blob/15240bb8979a8fe7b7b1bfa8309f1a78ca9fb35b/src/types/base.ts#L244)

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

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/15240bb8979a8fe7b7b1bfa8309f1a78ca9fb35b/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/15240bb8979a8fe7b7b1bfa8309f1a78ca9fb35b/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/15240bb8979a8fe7b7b1bfa8309f1a78ca9fb35b/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"PatternInt"` \| `"PatInt"`

Defined in: [types/base.ts:245](https://github.com/wontlost-ltd/aster-lang/blob/15240bb8979a8fe7b7b1bfa8309f1a78ca9fb35b/src/types/base.ts#L245)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `number`

Defined in: [types/base.ts:246](https://github.com/wontlost-ltd/aster-lang/blob/15240bb8979a8fe7b7b1bfa8309f1a78ca9fb35b/src/types/base.ts#L246)
