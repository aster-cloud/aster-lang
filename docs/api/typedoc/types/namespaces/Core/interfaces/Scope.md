[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Scope

Defined in: [types.ts:257](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types.ts#L257)

Scope 作用域基础接口。

## Extends

- [`BaseScope`](../../../base/interfaces/BaseScope.md)\<[`Origin`](../../../interfaces/Origin.md), [`Statement`](../type-aliases/Statement.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L32)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`span`](../../../base/interfaces/BaseScope.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L33)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`origin`](../../../base/interfaces/BaseScope.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L34)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`file`](../../../base/interfaces/BaseScope.md#file)

***

### kind

> `readonly` **kind**: `"Scope"`

Defined in: [types/base.ts:153](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L153)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`kind`](../../../base/interfaces/BaseScope.md#kind)

***

### statements

> `readonly` **statements**: readonly [`Statement`](../type-aliases/Statement.md)[]

Defined in: [types/base.ts:154](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L154)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`statements`](../../../base/interfaces/BaseScope.md#statements)
