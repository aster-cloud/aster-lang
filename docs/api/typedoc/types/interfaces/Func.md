[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Func

Defined in: [types.ts:169](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L169)

函数声明基础接口。

## Extends

- [`BaseFunc`](../base/interfaces/BaseFunc.md)\<[`Span`](Span.md), readonly `string`[], [`Type`](../type-aliases/Type.md)\>

## Properties

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:170](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L170)

***

### body

> `readonly` **body**: [`Block`](Block.md) \| `null`

Defined in: [types.ts:171](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L171)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:172](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L172)

#### Overrides

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`params`](../base/interfaces/BaseFunc.md#params)

***

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:173](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L173)

#### Overrides

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`span`](../base/interfaces/BaseFunc.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L42)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`origin`](../base/interfaces/BaseFunc.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L43)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`file`](../base/interfaces/BaseFunc.md#file)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types/base.ts:104](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L104)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`kind`](../namespaces/Core/interfaces/Func.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:105](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L105)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`name`](../namespaces/Core/interfaces/Func.md#name)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types/base.ts:106](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L106)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`typeParams`](../namespaces/Core/interfaces/Func.md#typeparams)

***

### effects

> `readonly` **effects**: readonly `string`[]

Defined in: [types/base.ts:108](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L108)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`effects`](../base/interfaces/BaseFunc.md#effects)

***

### effectCaps

> `readonly` **effectCaps**: readonly [`CapabilityKind`](../../config/semantic/enumerations/CapabilityKind.md)[]

Defined in: [types/base.ts:109](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L109)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`effectCaps`](../namespaces/Core/interfaces/Func.md#effectcaps)

***

### effectCapsExplicit

> `readonly` **effectCapsExplicit**: `boolean`

Defined in: [types/base.ts:110](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L110)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`effectCapsExplicit`](../namespaces/Core/interfaces/Func.md#effectcapsexplicit)
