[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: PatternCtor

Defined in: [types.ts:224](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types.ts#L224)

构造器模式基础接口。

## Extends

- [`BasePatternCtor`](../base/interfaces/BasePatternCtor.md)\<[`Span`](Span.md), [`Pattern`](../type-aliases/Pattern.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:225](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types.ts#L225)

#### Overrides

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`span`](../base/interfaces/BasePatternCtor.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L42)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`origin`](../base/interfaces/BasePatternCtor.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L43)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`file`](../base/interfaces/BasePatternCtor.md#file)

***

### kind

> `readonly` **kind**: `"PatternCtor"` \| `"PatCtor"`

Defined in: [types/base.ts:227](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L227)

#### Inherited from

[`PatCtor`](../namespaces/Core/interfaces/PatCtor.md).[`kind`](../namespaces/Core/interfaces/PatCtor.md#kind)

***

### typeName

> `readonly` **typeName**: `string`

Defined in: [types/base.ts:228](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L228)

#### Inherited from

[`PatCtor`](../namespaces/Core/interfaces/PatCtor.md).[`typeName`](../namespaces/Core/interfaces/PatCtor.md#typename)

***

### names

> `readonly` **names**: readonly `string`[]

Defined in: [types/base.ts:229](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L229)

#### Inherited from

[`PatCtor`](../namespaces/Core/interfaces/PatCtor.md).[`names`](../namespaces/Core/interfaces/PatCtor.md#names)

***

### args?

> `readonly` `optional` **args**: readonly [`Pattern`](../type-aliases/Pattern.md)[]

Defined in: [types/base.ts:230](https://github.com/wontlost-ltd/aster-lang/blob/f7cb9921608f3ae45a73f2ee7f3ec458307b0f7f/src/types/base.ts#L230)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`args`](../base/interfaces/BasePatternCtor.md#args)
