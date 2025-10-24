[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: PatternCtor

Defined in: [types.ts:224](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L224)

构造器模式基础接口。

## Extends

- [`BasePatternCtor`](../base/interfaces/BasePatternCtor.md)\<[`Span`](Span.md), [`Pattern`](../type-aliases/Pattern.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:225](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L225)

#### Overrides

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`span`](../base/interfaces/BasePatternCtor.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L35)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`origin`](../base/interfaces/BasePatternCtor.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L36)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`file`](../base/interfaces/BasePatternCtor.md#file)

***

### kind

> `readonly` **kind**: `"PatternCtor"` \| `"PatCtor"`

Defined in: [types/base.ts:220](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L220)

#### Inherited from

[`PatCtor`](../namespaces/Core/interfaces/PatCtor.md).[`kind`](../namespaces/Core/interfaces/PatCtor.md#kind)

***

### typeName

> `readonly` **typeName**: `string`

Defined in: [types/base.ts:221](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L221)

#### Inherited from

[`PatCtor`](../namespaces/Core/interfaces/PatCtor.md).[`typeName`](../namespaces/Core/interfaces/PatCtor.md#typename)

***

### names

> `readonly` **names**: readonly `string`[]

Defined in: [types/base.ts:222](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L222)

#### Inherited from

[`PatCtor`](../namespaces/Core/interfaces/PatCtor.md).[`names`](../namespaces/Core/interfaces/PatCtor.md#names)

***

### args?

> `readonly` `optional` **args**: readonly [`Pattern`](../type-aliases/Pattern.md)[]

Defined in: [types/base.ts:223](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L223)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`args`](../base/interfaces/BasePatternCtor.md#args)
