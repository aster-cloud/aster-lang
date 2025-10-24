[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Scope

Defined in: [types.ts:431](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L431)

Scope 作用域基础接口。

## Extends

- [`BaseScope`](../../../base/interfaces/BaseScope.md)\<[`Origin`](../../../interfaces/Origin.md), [`Statement`](../type-aliases/Statement.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L34)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`span`](../../../base/interfaces/BaseScope.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L35)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`origin`](../../../base/interfaces/BaseScope.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L36)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`file`](../../../base/interfaces/BaseScope.md#file)

***

### kind

> `readonly` **kind**: `"Scope"`

Defined in: [types/base.ts:156](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L156)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`kind`](../../../base/interfaces/BaseScope.md#kind)

***

### statements

> `readonly` **statements**: readonly [`Statement`](../type-aliases/Statement.md)[]

Defined in: [types/base.ts:157](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L157)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`statements`](../../../base/interfaces/BaseScope.md#statements)
