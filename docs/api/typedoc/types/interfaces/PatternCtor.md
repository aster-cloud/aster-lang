[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: PatternCtor

Defined in: [types.ts:251](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types.ts#L251)

构造器模式基础接口。

## Extends

- [`BasePatternCtor`](../base/interfaces/BasePatternCtor.md)\<[`Span`](Span.md), [`Pattern`](../type-aliases/Pattern.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:252](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types.ts#L252)

#### Overrides

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`span`](../base/interfaces/BasePatternCtor.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L42)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`origin`](../base/interfaces/BasePatternCtor.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L43)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`file`](../base/interfaces/BasePatternCtor.md#file)

***

### kind

> `readonly` **kind**: `"PatternCtor"` \| `"PatCtor"`

Defined in: [types/base.ts:268](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L268)

#### Inherited from

[`PatCtor`](../namespaces/Core/interfaces/PatCtor.md).[`kind`](../namespaces/Core/interfaces/PatCtor.md#kind)

***

### typeName

> `readonly` **typeName**: `string`

Defined in: [types/base.ts:269](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L269)

#### Inherited from

[`PatCtor`](../namespaces/Core/interfaces/PatCtor.md).[`typeName`](../namespaces/Core/interfaces/PatCtor.md#typename)

***

### names

> `readonly` **names**: readonly `string`[]

Defined in: [types/base.ts:270](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L270)

#### Inherited from

[`PatCtor`](../namespaces/Core/interfaces/PatCtor.md).[`names`](../namespaces/Core/interfaces/PatCtor.md#names)

***

### args?

> `readonly` `optional` **args**: readonly [`Pattern`](../type-aliases/Pattern.md)[]

Defined in: [types/base.ts:271](https://github.com/wontlost-ltd/aster-lang/blob/3875b263f60d4a4862bc2349e02db9d22be4056b/src/types/base.ts#L271)

#### Inherited from

[`BasePatternCtor`](../base/interfaces/BasePatternCtor.md).[`args`](../base/interfaces/BasePatternCtor.md#args)
