[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseFunc\<S, E, T\>

Defined in: [types/base.ts:94](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L94)

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

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types/base.ts:95](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L95)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:96](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L96)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types/base.ts:97](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L97)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](BaseParameter.md)\<`T`\>[]

Defined in: [types/base.ts:98](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L98)

***

### effects?

> `readonly` `optional` **effects**: `E`

Defined in: [types/base.ts:99](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L99)

***

### effectCaps?

> `readonly` `optional` **effectCaps**: readonly [`CapabilityKind`](../../../config/semantic/enumerations/CapabilityKind.md)[]

Defined in: [types/base.ts:100](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L100)

***

### effectCapsExplicit?

> `readonly` `optional` **effectCapsExplicit**: `boolean`

Defined in: [types/base.ts:101](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L101)
