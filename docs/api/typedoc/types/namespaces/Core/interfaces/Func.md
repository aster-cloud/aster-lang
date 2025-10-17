[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Func

Defined in: [types.ts:330](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L330)

函数声明基础接口。

## Extends

- [`BaseFunc`](../../../base/interfaces/BaseFunc.md)\<[`Origin`](../../../interfaces/Origin.md), readonly [`Effect`](../../../../config/semantic/enumerations/Effect.md)[], [`Type`](../type-aliases/Type.md)\>

## Properties

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:331](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L331)

***

### effects

> `readonly` **effects**: readonly [`Effect`](../../../../config/semantic/enumerations/Effect.md)[]

Defined in: [types.ts:332](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L332)

#### Overrides

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effects`](../../../base/interfaces/BaseFunc.md#effects)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types.ts:333](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L333)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:334](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L334)

#### Overrides

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`params`](../../../base/interfaces/BaseFunc.md#params)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L32)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`span`](../../../base/interfaces/BaseFunc.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L33)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`origin`](../../../base/interfaces/BaseFunc.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L34)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`file`](../../../base/interfaces/BaseFunc.md#file)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types/base.ts:95](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L95)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`kind`](../../../base/interfaces/BaseFunc.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:96](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L96)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`name`](../../../base/interfaces/BaseFunc.md#name)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types/base.ts:97](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L97)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`typeParams`](../../../base/interfaces/BaseFunc.md#typeparams)

***

### effectCaps?

> `readonly` `optional` **effectCaps**: readonly [`CapabilityKind`](../../../../config/semantic/enumerations/CapabilityKind.md)[]

Defined in: [types/base.ts:100](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L100)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effectCaps`](../../../base/interfaces/BaseFunc.md#effectcaps)

***

### effectCapsExplicit?

> `readonly` `optional` **effectCapsExplicit**: `boolean`

Defined in: [types/base.ts:101](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L101)

#### Inherited from

[`BaseFunc`](../../../base/interfaces/BaseFunc.md).[`effectCapsExplicit`](../../../base/interfaces/BaseFunc.md#effectcapsexplicit)
