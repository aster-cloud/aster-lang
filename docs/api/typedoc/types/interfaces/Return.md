[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Return

Defined in: [types.ts:195](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types.ts#L195)

Return 语句基础接口。

## Extends

- [`BaseReturn`](../base/interfaces/BaseReturn.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:196](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types.ts#L196)

#### Overrides

[`BaseReturn`](../base/interfaces/BaseReturn.md).[`span`](../base/interfaces/BaseReturn.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L42)

#### Inherited from

[`BaseReturn`](../base/interfaces/BaseReturn.md).[`origin`](../base/interfaces/BaseReturn.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L43)

#### Inherited from

[`BaseReturn`](../base/interfaces/BaseReturn.md).[`file`](../base/interfaces/BaseReturn.md#file)

***

### kind

> `readonly` **kind**: `"Return"`

Defined in: [types/base.ts:147](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L147)

#### Inherited from

[`Return`](../namespaces/Core/interfaces/Return.md).[`kind`](../namespaces/Core/interfaces/Return.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:148](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L148)

#### Inherited from

[`BaseReturn`](../base/interfaces/BaseReturn.md).[`expr`](../base/interfaces/BaseReturn.md#expr-1)
