[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Call

Defined in: [types.ts:285](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L285)

函数调用基础接口。

## Extends

- [`BaseCall`](../base/interfaces/BaseCall.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:286](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L286)

#### Overrides

[`BaseCall`](../base/interfaces/BaseCall.md).[`span`](../base/interfaces/BaseCall.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L35)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`origin`](../base/interfaces/BaseCall.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L36)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`file`](../base/interfaces/BaseCall.md#file)

***

### kind

> `readonly` **kind**: `"Call"`

Defined in: [types/base.ts:308](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L308)

#### Inherited from

[`Call`](../namespaces/Core/interfaces/Call.md).[`kind`](../namespaces/Core/interfaces/Call.md#kind)

***

### target

> `readonly` **target**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:309](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L309)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`target`](../base/interfaces/BaseCall.md#target)

***

### args

> `readonly` **args**: readonly [`Expression`](../type-aliases/Expression.md)[]

Defined in: [types/base.ts:310](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L310)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`args`](../base/interfaces/BaseCall.md#args)
