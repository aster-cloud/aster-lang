[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Func

Defined in: [types.ts:282](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types.ts#L282)

函数声明基础接口。

## Extends

- [`BaseFunc`](../../../base/interfaces/BaseFunc.md)\<[`Origin`](../../../interfaces/Origin.md), readonly [`Effect`](../../../../config/semantic/enumerations/Effect.md)[], [`Type`](../type-aliases/Type.md)\>

## Properties

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:283](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types.ts#L283)

***

### effects

> `readonly` **effects**: readonly [`Effect`](../../../../config/semantic/enumerations/Effect.md)[]

Defined in: [types.ts:284](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types.ts#L284)

#### Overrides

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effects`](../../../base/interfaces/BaseFunc.md#effects)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types.ts:285](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types.ts#L285)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L32)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`span`](../../../base/interfaces/BaseFunc.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L33)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`origin`](../../../base/interfaces/BaseFunc.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L34)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`file`](../../../base/interfaces/BaseFunc.md#file)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types/base.ts:95](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L95)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`kind`](../../../base/interfaces/BaseFunc.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:96](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L96)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`name`](../../../base/interfaces/BaseFunc.md#name)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types/base.ts:97](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L97)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`typeParams`](../../../base/interfaces/BaseFunc.md#typeparams)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](../../../base/interfaces/BaseParameter.md)\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:98](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L98)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`params`](../../../base/interfaces/BaseFunc.md#params)

***

### effectCaps?

> `readonly` `optional` **effectCaps**: readonly [`CapabilityKind`](../../../../config/semantic/enumerations/CapabilityKind.md)[]

Defined in: [types/base.ts:100](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L100)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effectCaps`](../../../base/interfaces/BaseFunc.md#effectcaps)

***

### effectCapsExplicit?

> `readonly` `optional` **effectCapsExplicit**: `boolean`

Defined in: [types/base.ts:101](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L101)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effectCapsExplicit`](../../../base/interfaces/BaseFunc.md#effectcapsexplicit)
