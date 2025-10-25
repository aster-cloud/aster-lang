[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Let

Defined in: [types.ts:186](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L186)

Let 绑定基础接口。

## Extends

- [`BaseLet`](../base/interfaces/BaseLet.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:187](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L187)

#### Overrides

[`BaseLet`](../base/interfaces/BaseLet.md).[`span`](../base/interfaces/BaseLet.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L35)

#### Inherited from

[`BaseLet`](../base/interfaces/BaseLet.md).[`origin`](../base/interfaces/BaseLet.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L36)

#### Inherited from

[`BaseLet`](../base/interfaces/BaseLet.md).[`file`](../base/interfaces/BaseLet.md#file)

***

### kind

> `readonly` **kind**: `"Let"`

Defined in: [types/base.ts:122](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L122)

#### Inherited from

[`Let`](../namespaces/Core/interfaces/Let.md).[`kind`](../namespaces/Core/interfaces/Let.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:123](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L123)

#### Inherited from

[`Let`](../namespaces/Core/interfaces/Let.md).[`name`](../namespaces/Core/interfaces/Let.md#name)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:124](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L124)

#### Inherited from

[`BaseLet`](../base/interfaces/BaseLet.md).[`expr`](../base/interfaces/BaseLet.md#expr-1)
