[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Call

Defined in: [types.ts:286](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types.ts#L286)

函数调用基础接口。

## Extends

- [`BaseCall`](../base/interfaces/BaseCall.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:287](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types.ts#L287)

#### Overrides

[`BaseCall`](../base/interfaces/BaseCall.md).[`span`](../base/interfaces/BaseCall.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L42)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`origin`](../base/interfaces/BaseCall.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L43)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`file`](../base/interfaces/BaseCall.md#file)

***

### kind

> `readonly` **kind**: `"Call"`

Defined in: [types/base.ts:315](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L315)

#### Inherited from

[`Call`](../namespaces/Core/interfaces/Call.md).[`kind`](../namespaces/Core/interfaces/Call.md#kind)

***

### target

> `readonly` **target**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:316](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L316)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`target`](../base/interfaces/BaseCall.md#target)

***

### args

> `readonly` **args**: readonly [`Expression`](../type-aliases/Expression.md)[]

Defined in: [types/base.ts:317](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types/base.ts#L317)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`args`](../base/interfaces/BaseCall.md#args)
