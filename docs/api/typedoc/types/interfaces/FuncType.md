[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: FuncType

Defined in: [types.ts:441](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types.ts#L441)

函数类型基础接口。

## Extends

- [`BaseFuncType`](../base/interfaces/BaseFuncType.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### effectParams?

> `readonly` `optional` **effectParams**: readonly [`EffectVar`](EffectVar.md)[]

Defined in: [types.ts:442](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types.ts#L442)

***

### declaredEffects?

> `readonly` `optional` **declaredEffects**: readonly ([`Effect`](../../config/semantic/enumerations/Effect.md) \| [`EffectVar`](EffectVar.md))[]

Defined in: [types.ts:443](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types.ts#L443)

***

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:444](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types.ts#L444)

#### Overrides

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`span`](../base/interfaces/BaseFuncType.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L42)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`origin`](../base/interfaces/BaseFuncType.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L43)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`file`](../base/interfaces/BaseFuncType.md#file)

***

### kind

> `readonly` **kind**: `"FuncType"`

Defined in: [types/base.ts:502](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L502)

#### Inherited from

[`FuncType`](../namespaces/Core/interfaces/FuncType.md).[`kind`](../namespaces/Core/interfaces/FuncType.md#kind)

***

### params

> `readonly` **params**: readonly [`Type`](../type-aliases/Type.md)[]

Defined in: [types/base.ts:503](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L503)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`params`](../base/interfaces/BaseFuncType.md#params)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:504](https://github.com/wontlost-ltd/aster-lang/blob/27d194a2cb2576783cfe795e48f717a4ab5e63c2/src/types/base.ts#L504)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`ret`](../base/interfaces/BaseFuncType.md#ret)
