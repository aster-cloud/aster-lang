[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BasePatternNull\<S\>

Defined in: [types/base.ts:259](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L259)

Null 模式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`PatternNull`](../../interfaces/PatternNull.md)
- [`PatNull`](../../namespaces/Core/interfaces/PatNull.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"PatternNull"` \| `"PatNull"`

Defined in: [types/base.ts:260](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L260)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)
