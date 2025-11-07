[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: PatCtor

Defined in: [types.ts:453](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types.ts#L453)

构造器模式基础接口。

## Extends

- [`BasePatternCtor`](../../../base/interfaces/BasePatternCtor.md)\<[`Origin`](../../../interfaces/Origin.md), [`Pattern`](../type-aliases/Pattern.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types/base.ts#L41)

#### Inherited from

[`BasePatternCtor`](../../../base/interfaces/BasePatternCtor.md).[`span`](../../../base/interfaces/BasePatternCtor.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types/base.ts#L42)

#### Inherited from

[`BasePatternCtor`](../../../base/interfaces/BasePatternCtor.md).[`origin`](../../../base/interfaces/BasePatternCtor.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types/base.ts#L43)

#### Inherited from

[`BasePatternCtor`](../../../base/interfaces/BasePatternCtor.md).[`file`](../../../base/interfaces/BasePatternCtor.md#file)

***

### kind

> `readonly` **kind**: `"PatternCtor"` \| `"PatCtor"`

Defined in: [types/base.ts:227](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types/base.ts#L227)

#### Inherited from

[`BasePatternCtor`](../../../base/interfaces/BasePatternCtor.md).[`kind`](../../../base/interfaces/BasePatternCtor.md#kind)

***

### typeName

> `readonly` **typeName**: `string`

Defined in: [types/base.ts:228](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types/base.ts#L228)

#### Inherited from

[`BasePatternCtor`](../../../base/interfaces/BasePatternCtor.md).[`typeName`](../../../base/interfaces/BasePatternCtor.md#typename)

***

### names

> `readonly` **names**: readonly `string`[]

Defined in: [types/base.ts:229](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types/base.ts#L229)

#### Inherited from

[`BasePatternCtor`](../../../base/interfaces/BasePatternCtor.md).[`names`](../../../base/interfaces/BasePatternCtor.md#names)

***

### args?

> `readonly` `optional` **args**: readonly [`Pattern`](../type-aliases/Pattern.md)[]

Defined in: [types/base.ts:230](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types/base.ts#L230)

#### Inherited from

[`BasePatternCtor`](../../../base/interfaces/BasePatternCtor.md).[`args`](../../../base/interfaces/BasePatternCtor.md#args)
