[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Func

Defined in: [types.ts:162](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types.ts#L162)

函数声明基础接口。

## Extends

- [`BaseFunc`](../base/interfaces/BaseFunc.md)\<[`Span`](Span.md), readonly `string`[], [`Type`](../type-aliases/Type.md)\>

## Properties

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:163](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types.ts#L163)

***

### body

> `readonly` **body**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:164](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types.ts#L164)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:165](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types.ts#L165)

#### Overrides

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`params`](../base/interfaces/BaseFunc.md#params)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L32)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`span`](../base/interfaces/BaseFunc.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L33)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`origin`](../base/interfaces/BaseFunc.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L34)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`file`](../base/interfaces/BaseFunc.md#file)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types/base.ts:95](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L95)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`kind`](../namespaces/Core/interfaces/Func.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:96](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L96)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`name`](../namespaces/Core/interfaces/Func.md#name)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types/base.ts:97](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L97)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`typeParams`](../namespaces/Core/interfaces/Func.md#typeparams)

***

### effects?

> `readonly` `optional` **effects**: readonly `string`[]

Defined in: [types/base.ts:99](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L99)

#### Inherited from

[`BaseFunc`](../base/interfaces/BaseFunc.md).[`effects`](../base/interfaces/BaseFunc.md#effects)

***

### effectCaps?

> `readonly` `optional` **effectCaps**: readonly [`CapabilityKind`](../../config/semantic/enumerations/CapabilityKind.md)[]

Defined in: [types/base.ts:100](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L100)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`effectCaps`](../namespaces/Core/interfaces/Func.md#effectcaps)

***

### effectCapsExplicit?

> `readonly` `optional` **effectCapsExplicit**: `boolean`

Defined in: [types/base.ts:101](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types/base.ts#L101)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`effectCapsExplicit`](../namespaces/Core/interfaces/Func.md#effectcapsexplicit)
