[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Call

Defined in: [types.ts:488](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types.ts#L488)

函数调用基础接口。

## Extends

- [`BaseCall`](../../../base/interfaces/BaseCall.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L34)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`span`](../../../base/interfaces/BaseCall.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L35)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`origin`](../../../base/interfaces/BaseCall.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L36)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`file`](../../../base/interfaces/BaseCall.md#file)

***

### kind

> `readonly` **kind**: `"Call"`

Defined in: [types/base.ts:308](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L308)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`kind`](../../../base/interfaces/BaseCall.md#kind)

***

### target

> `readonly` **target**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:309](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L309)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`target`](../../../base/interfaces/BaseCall.md#target)

***

### args

> `readonly` **args**: readonly [`Expression`](../type-aliases/Expression.md)[]

Defined in: [types/base.ts:310](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L310)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`args`](../../../base/interfaces/BaseCall.md#args)
