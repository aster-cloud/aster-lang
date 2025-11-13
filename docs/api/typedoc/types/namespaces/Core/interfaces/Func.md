[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Func

Defined in: [types.ts:440](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types.ts#L440)

函数声明基础接口。

## Extends

- [`BaseFunc`](../../../base/interfaces/BaseFunc.md)\<[`Origin`](../../../interfaces/Origin.md), readonly [`Effect`](../../../../config/semantic/enumerations/Effect.md)[], [`Type`](../type-aliases/Type.md)\>

## Properties

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:441](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types.ts#L441)

***

### effects

> `readonly` **effects**: readonly [`Effect`](../../../../config/semantic/enumerations/Effect.md)[]

Defined in: [types.ts:442](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types.ts#L442)

#### Overrides

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effects`](../../../base/interfaces/BaseFunc.md#effects)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types.ts:443](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types.ts#L443)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:444](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types.ts#L444)

#### Overrides

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`params`](../../../base/interfaces/BaseFunc.md#params)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types/base.ts#L41)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`span`](../../../base/interfaces/BaseFunc.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types/base.ts#L42)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`origin`](../../../base/interfaces/BaseFunc.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types/base.ts#L43)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`file`](../../../base/interfaces/BaseFunc.md#file)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types/base.ts:104](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types/base.ts#L104)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`kind`](../../../base/interfaces/BaseFunc.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:105](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types/base.ts#L105)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`name`](../../../base/interfaces/BaseFunc.md#name)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types/base.ts:106](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types/base.ts#L106)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`typeParams`](../../../base/interfaces/BaseFunc.md#typeparams)

***

### effectCaps

> `readonly` **effectCaps**: readonly [`CapabilityKind`](../../../../config/semantic/enumerations/CapabilityKind.md)[]

Defined in: [types/base.ts:109](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types/base.ts#L109)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effectCaps`](../../../base/interfaces/BaseFunc.md#effectcaps)

***

### effectCapsExplicit

> `readonly` **effectCapsExplicit**: `boolean`

Defined in: [types/base.ts:110](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types/base.ts#L110)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effectCapsExplicit`](../../../base/interfaces/BaseFunc.md#effectcapsexplicit)
