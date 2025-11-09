[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Err

Defined in: [types.ts:333](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types.ts#L333)

Err 表达式基础接口。

## Extends

- [`BaseErr`](../base/interfaces/BaseErr.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:334](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types.ts#L334)

#### Overrides

[`BaseErr`](../base/interfaces/BaseErr.md).[`span`](../base/interfaces/BaseErr.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L42)

#### Inherited from

[`BaseErr`](../base/interfaces/BaseErr.md).[`origin`](../base/interfaces/BaseErr.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L43)

#### Inherited from

[`BaseErr`](../base/interfaces/BaseErr.md).[`file`](../base/interfaces/BaseErr.md#file)

***

### kind

> `readonly` **kind**: `"Err"`

Defined in: [types/base.ts:399](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L399)

#### Inherited from

[`Err`](../namespaces/Core/interfaces/Err.md).[`kind`](../namespaces/Core/interfaces/Err.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:400](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L400)

#### Inherited from

[`BaseErr`](../base/interfaces/BaseErr.md).[`expr`](../base/interfaces/BaseErr.md#expr-1)
