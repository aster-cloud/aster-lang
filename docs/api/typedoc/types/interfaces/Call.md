[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Call

Defined in: [types.ts:198](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types.ts#L198)

函数调用基础接口。

## Extends

- [`BaseCall`](../base/interfaces/BaseCall.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L32)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`span`](../base/interfaces/BaseCall.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L33)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`origin`](../base/interfaces/BaseCall.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L34)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`file`](../base/interfaces/BaseCall.md#file)

***

### kind

> `readonly` **kind**: `"Call"`

Defined in: [types/base.ts:303](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L303)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`kind`](../base/interfaces/BaseCall.md#kind)

***

### target

> `readonly` **target**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:304](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L304)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`target`](../base/interfaces/BaseCall.md#target)

***

### args

> `readonly` **args**: readonly [`Expression`](../type-aliases/Expression.md)[]

Defined in: [types/base.ts:305](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L305)

#### Inherited from

[`BaseCall`](../base/interfaces/BaseCall.md).[`args`](../base/interfaces/BaseCall.md#args)
