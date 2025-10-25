[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Func

Defined in: [types.ts:166](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L166)

函数声明基础接口。

## Extends

- [`BaseFunc`](../base/interfaces/BaseFunc.md)\<[`Span`](Span.md), readonly `string`[], [`Type`](../type-aliases/Type.md)\>

## Properties

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:167](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L167)

***

### body

> `readonly` **body**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:168](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L168)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:169](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L169)

#### Overrides

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`params`](../base/interfaces/BaseFunc.md#params)

***

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:170](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L170)

#### Overrides

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`span`](../base/interfaces/BaseFunc.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L35)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`origin`](../base/interfaces/BaseFunc.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L36)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`file`](../base/interfaces/BaseFunc.md#file)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types/base.ts:97](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L97)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`kind`](../namespaces/Core/interfaces/Func.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:98](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L98)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`name`](../namespaces/Core/interfaces/Func.md#name)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types/base.ts:99](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L99)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`typeParams`](../namespaces/Core/interfaces/Func.md#typeparams)

***

### effects

> `readonly` **effects**: readonly `string`[]

Defined in: [types/base.ts:101](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L101)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`effects`](../base/interfaces/BaseFunc.md#effects)

***

### effectCaps

> `readonly` **effectCaps**: readonly [`CapabilityKind`](../../config/semantic/enumerations/CapabilityKind.md)[]

Defined in: [types/base.ts:102](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L102)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`effectCaps`](../namespaces/Core/interfaces/Func.md#effectcaps)

***

### effectCapsExplicit

> `readonly` **effectCapsExplicit**: `boolean`

Defined in: [types/base.ts:103](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L103)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`effectCapsExplicit`](../namespaces/Core/interfaces/Func.md#effectcapsexplicit)
