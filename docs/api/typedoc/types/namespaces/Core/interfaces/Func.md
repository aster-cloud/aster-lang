[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Func

Defined in: [types.ts:470](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types.ts#L470)

函数声明基础接口。

## Extends

- [`BaseFunc`](../../../base/interfaces/BaseFunc.md)\<[`Origin`](../../../interfaces/Origin.md), readonly [`Effect`](../../../../config/semantic/enumerations/Effect.md)[], [`Type`](../type-aliases/Type.md)\>

## Properties

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:471](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types.ts#L471)

***

### effects

> `readonly` **effects**: readonly [`Effect`](../../../../config/semantic/enumerations/Effect.md)[]

Defined in: [types.ts:472](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types.ts#L472)

#### Overrides

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effects`](../../../base/interfaces/BaseFunc.md#effects)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types.ts:473](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types.ts#L473)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:474](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types.ts#L474)

#### Overrides

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`params`](../../../base/interfaces/BaseFunc.md#params)

***

### effectParams?

> `readonly` `optional` **effectParams**: readonly `string`[]

Defined in: [types.ts:475](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types.ts#L475)

***

### declaredEffects?

> `readonly` `optional` **declaredEffects**: readonly ([`EffectVar`](EffectVar.md) \| [`Effect`](../../../../config/semantic/enumerations/Effect.md))[]

Defined in: [types.ts:476](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types.ts#L476)

***

### piiLevel?

> `readonly` `optional` **piiLevel**: [`PiiSensitivityLevel`](../../../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:477](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types.ts#L477)

***

### piiCategories?

> `readonly` `optional` **piiCategories**: readonly `string`[]

Defined in: [types.ts:478](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types.ts#L478)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L41)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`span`](../../../base/interfaces/BaseFunc.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L42)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`origin`](../../../base/interfaces/BaseFunc.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L43)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`file`](../../../base/interfaces/BaseFunc.md#file)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types/base.ts:104](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L104)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`kind`](../../../base/interfaces/BaseFunc.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:105](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L105)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`name`](../../../base/interfaces/BaseFunc.md#name)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types/base.ts:106](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L106)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`typeParams`](../../../base/interfaces/BaseFunc.md#typeparams)

***

### effectCaps

> `readonly` **effectCaps**: readonly [`CapabilityKind`](../../../../config/semantic/enumerations/CapabilityKind.md)[]

Defined in: [types/base.ts:109](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L109)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effectCaps`](../../../base/interfaces/BaseFunc.md#effectcaps)

***

### effectCapsExplicit

> `readonly` **effectCapsExplicit**: `boolean`

Defined in: [types/base.ts:110](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L110)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effectCapsExplicit`](../../../base/interfaces/BaseFunc.md#effectcapsexplicit)
