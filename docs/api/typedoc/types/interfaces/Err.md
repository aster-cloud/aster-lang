[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Err

Defined in: [types.ts:334](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types.ts#L334)

Err 表达式基础接口。

## Extends

- [`BaseErr`](../base/interfaces/BaseErr.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:335](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types.ts#L335)

#### Overrides

[`BaseErr`](../base/interfaces/BaseErr.md).[`span`](../base/interfaces/BaseErr.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L42)

#### Inherited from

[`BaseErr`](../base/interfaces/BaseErr.md).[`origin`](../base/interfaces/BaseErr.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L43)

#### Inherited from

[`BaseErr`](../base/interfaces/BaseErr.md).[`file`](../base/interfaces/BaseErr.md#file)

***

### kind

> `readonly` **kind**: `"Err"`

Defined in: [types/base.ts:400](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L400)

#### Inherited from

[`Err`](../namespaces/Core/interfaces/Err.md).[`kind`](../namespaces/Core/interfaces/Err.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:401](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L401)

#### Inherited from

[`BaseErr`](../base/interfaces/BaseErr.md).[`expr`](../base/interfaces/BaseErr.md#expr-1)
