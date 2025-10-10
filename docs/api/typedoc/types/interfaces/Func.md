[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Func

Defined in: [types.ts:126](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types.ts#L126)

函数声明基础接口。

## Extends

- [`BaseFunc`](../base/interfaces/BaseFunc.md)\<[`Span`](Span.md), readonly `string`[], [`Type`](../type-aliases/Type.md)\>

## Properties

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:127](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types.ts#L127)

***

### body

> `readonly` **body**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:128](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types.ts#L128)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L32)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`span`](../base/interfaces/BaseFunc.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L33)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`origin`](../base/interfaces/BaseFunc.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L34)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`file`](../base/interfaces/BaseFunc.md#file)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types/base.ts:95](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L95)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`kind`](../namespaces/Core/interfaces/Func.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:96](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L96)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`name`](../namespaces/Core/interfaces/Func.md#name)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types/base.ts:97](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L97)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`typeParams`](../namespaces/Core/interfaces/Func.md#typeparams)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](../base/interfaces/BaseParameter.md)\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:98](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L98)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`params`](../base/interfaces/BaseFunc.md#params)

***

### effects?

> `readonly` `optional` **effects**: readonly `string`[]

Defined in: [types/base.ts:99](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L99)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`effects`](../base/interfaces/BaseFunc.md#effects)

***

### effectCaps?

> `readonly` `optional` **effectCaps**: readonly [`CapabilityKind`](../../config/semantic/enumerations/CapabilityKind.md)[]

Defined in: [types/base.ts:100](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L100)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`effectCaps`](../namespaces/Core/interfaces/Func.md#effectcaps)

***

### effectCapsExplicit?

> `readonly` `optional` **effectCapsExplicit**: `boolean`

Defined in: [types/base.ts:101](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L101)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`effectCapsExplicit`](../namespaces/Core/interfaces/Func.md#effectcapsexplicit)
