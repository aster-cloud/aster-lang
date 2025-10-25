[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Func

Defined in: [types.ts:413](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L413)

函数声明基础接口。

## Extends

- [`BaseFunc`](../../../base/interfaces/BaseFunc.md)\<[`Origin`](../../../interfaces/Origin.md), readonly [`Effect`](../../../../config/semantic/enumerations/Effect.md)[], [`Type`](../type-aliases/Type.md)\>

## Properties

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:414](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L414)

***

### effects

> `readonly` **effects**: readonly [`Effect`](../../../../config/semantic/enumerations/Effect.md)[]

Defined in: [types.ts:415](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L415)

#### Overrides

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effects`](../../../base/interfaces/BaseFunc.md#effects)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types.ts:416](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L416)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:417](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L417)

#### Overrides

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`params`](../../../base/interfaces/BaseFunc.md#params)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L34)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`span`](../../../base/interfaces/BaseFunc.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L35)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`origin`](../../../base/interfaces/BaseFunc.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L36)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`file`](../../../base/interfaces/BaseFunc.md#file)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types/base.ts:97](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L97)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`kind`](../../../base/interfaces/BaseFunc.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:98](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L98)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`name`](../../../base/interfaces/BaseFunc.md#name)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types/base.ts:99](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L99)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`typeParams`](../../../base/interfaces/BaseFunc.md#typeparams)

***

### effectCaps

> `readonly` **effectCaps**: readonly [`CapabilityKind`](../../../../config/semantic/enumerations/CapabilityKind.md)[]

Defined in: [types/base.ts:102](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L102)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effectCaps`](../../../base/interfaces/BaseFunc.md#effectcaps)

***

### effectCapsExplicit

> `readonly` **effectCapsExplicit**: `boolean`

Defined in: [types/base.ts:103](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L103)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effectCapsExplicit`](../../../base/interfaces/BaseFunc.md#effectcapsexplicit)
