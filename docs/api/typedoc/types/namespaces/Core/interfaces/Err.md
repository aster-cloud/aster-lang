[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Err

Defined in: [types.ts:562](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L562)

Err 表达式基础接口。

## Extends

- [`BaseErr`](../../../base/interfaces/BaseErr.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L41)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`span`](../../../base/interfaces/BaseErr.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L42)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`origin`](../../../base/interfaces/BaseErr.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L43)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`file`](../../../base/interfaces/BaseErr.md#file)

***

### kind

> `readonly` **kind**: `"Err"`

Defined in: [types/base.ts:400](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L400)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`kind`](../../../base/interfaces/BaseErr.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:401](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L401)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`expr`](../../../base/interfaces/BaseErr.md#expr-1)
