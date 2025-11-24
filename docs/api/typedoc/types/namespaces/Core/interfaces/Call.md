[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Call

Defined in: [types.ts:572](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/types.ts#L572)

函数调用基础接口。

## Extends

- [`BaseCall`](../../../base/interfaces/BaseCall.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/types/base.ts#L41)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`span`](../../../base/interfaces/BaseCall.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/types/base.ts#L42)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`origin`](../../../base/interfaces/BaseCall.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/types/base.ts#L43)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`file`](../../../base/interfaces/BaseCall.md#file)

***

### kind

> `readonly` **kind**: `"Call"`

Defined in: [types/base.ts:356](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/types/base.ts#L356)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`kind`](../../../base/interfaces/BaseCall.md#kind)

***

### target

> `readonly` **target**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:357](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/types/base.ts#L357)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`target`](../../../base/interfaces/BaseCall.md#target)

***

### args

> `readonly` **args**: readonly [`Expression`](../type-aliases/Expression.md)[]

Defined in: [types/base.ts:358](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/types/base.ts#L358)

#### Inherited from

[`BaseCall`](../../../base/interfaces/BaseCall.md).[`args`](../../../base/interfaces/BaseCall.md#args)
