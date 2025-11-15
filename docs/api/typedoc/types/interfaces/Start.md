[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Start

Defined in: [types.ts:224](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L224)

Start 异步任务基础接口。

## Extends

- [`BaseStart`](../base/interfaces/BaseStart.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:225](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L225)

#### Overrides

[`BaseStart`](../base/interfaces/BaseStart.md).[`span`](../base/interfaces/BaseStart.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L42)

#### Inherited from

[`BaseStart`](../base/interfaces/BaseStart.md).[`origin`](../base/interfaces/BaseStart.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L43)

#### Inherited from

[`BaseStart`](../base/interfaces/BaseStart.md).[`file`](../base/interfaces/BaseStart.md#file)

***

### kind

> `readonly` **kind**: `"Start"`

Defined in: [types/base.ts:199](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L199)

#### Inherited from

[`Start`](../namespaces/Core/interfaces/Start.md).[`kind`](../namespaces/Core/interfaces/Start.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:200](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L200)

#### Inherited from

[`Start`](../namespaces/Core/interfaces/Start.md).[`name`](../namespaces/Core/interfaces/Start.md#name)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:201](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L201)

#### Inherited from

[`BaseStart`](../base/interfaces/BaseStart.md).[`expr`](../base/interfaces/BaseStart.md#expr-1)
