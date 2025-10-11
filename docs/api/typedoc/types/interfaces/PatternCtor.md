[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: PatternCtor

Defined in: [types.ts:163](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types.ts#L163)

构造器模式基础接口。

## Extends

- [`BasePatternCtor`](../base/interfaces/BasePatternCtor.md)\<[`Span`](Span.md), [`Pattern`](../type-aliases/Pattern.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L32)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`span`](../base/interfaces/BasePatternCtor.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L33)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`origin`](../base/interfaces/BasePatternCtor.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L34)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`file`](../base/interfaces/BasePatternCtor.md#file)

***

### kind

> `readonly` **kind**: `"PatternCtor"` \| `"PatCtor"`

Defined in: [types/base.ts:218](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L218)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`kind`](../base/interfaces/BasePatternCtor.md#kind)

***

### typeName

> `readonly` **typeName**: `string`

Defined in: [types/base.ts:219](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L219)

#### Inherited from

[`PatCtor`](../namespaces/Core/interfaces/PatCtor.md).[`typeName`](../namespaces/Core/interfaces/PatCtor.md#typename)

***

### names

> `readonly` **names**: readonly `string`[]

Defined in: [types/base.ts:220](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L220)

#### Inherited from

[`PatCtor`](../namespaces/Core/interfaces/PatCtor.md).[`names`](../namespaces/Core/interfaces/PatCtor.md#names)

***

### args?

> `readonly` `optional` **args**: readonly [`Pattern`](../type-aliases/Pattern.md)[]

Defined in: [types/base.ts:221](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L221)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`args`](../base/interfaces/BasePatternCtor.md#args)
