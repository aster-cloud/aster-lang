[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseFunc\<S, E, T\>

Defined in: [types/base.ts:96](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L96)

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

> `readonly` `optional` **span**: `HasFileProp`\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `HasFileProp`\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L35)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L36)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types/base.ts:97](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L97)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:98](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L98)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types/base.ts:99](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L99)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](BaseParameter.md)\<`T`\>[]

Defined in: [types/base.ts:100](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L100)

***

### effects

> `readonly` **effects**: `E`

Defined in: [types/base.ts:101](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L101)

***

### effectCaps

> `readonly` **effectCaps**: readonly [`CapabilityKind`](../../../config/semantic/enumerations/CapabilityKind.md)[]

Defined in: [types/base.ts:102](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L102)

***

### effectCapsExplicit

> `readonly` **effectCapsExplicit**: `boolean`

Defined in: [types/base.ts:103](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L103)
