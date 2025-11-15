[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseFunc\<S, E, T\>

Defined in: [types/base.ts:103](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L103)

函数声明基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Func`](../../interfaces/Func.md)
- [`Func`](../../namespaces/Core/interfaces/Func.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

Span 类型

### E

`E` = `string`[] \| readonly [`Effect`](../../../config/semantic/enumerations/Effect.md)[]

Effect 类型（AST: string[], Core: Effect[]）

### T

`T` = `unknown`

Type 节点类型

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types/base.ts:104](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L104)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:105](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L105)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types/base.ts:106](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L106)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](BaseParameter.md)\<`T`\>[]

Defined in: [types/base.ts:107](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L107)

***

### effects

> `readonly` **effects**: `E`

Defined in: [types/base.ts:108](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L108)

***

### effectCaps

> `readonly` **effectCaps**: readonly [`CapabilityKind`](../../../config/semantic/enumerations/CapabilityKind.md)[]

Defined in: [types/base.ts:109](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L109)

***

### effectCapsExplicit

> `readonly` **effectCapsExplicit**: `boolean`

Defined in: [types/base.ts:110](https://github.com/wontlost-ltd/aster-lang/blob/5aed71e244ba99d51d83b6e3bb0e5f93667094c5/src/types/base.ts#L110)
