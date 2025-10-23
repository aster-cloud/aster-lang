[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Return

Defined in: [types.ts:194](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L194)

Return 语句基础接口。

## Extends

- [`BaseReturn`](../base/interfaces/BaseReturn.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:195](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L195)

#### Overrides

[`BaseReturn`](../base/interfaces/BaseReturn.md).[`span`](../base/interfaces/BaseReturn.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L35)

#### Inherited from

[`BaseReturn`](../base/interfaces/BaseReturn.md).[`origin`](../base/interfaces/BaseReturn.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L36)

#### Inherited from

[`BaseReturn`](../base/interfaces/BaseReturn.md).[`file`](../base/interfaces/BaseReturn.md#file)

***

### kind

> `readonly` **kind**: `"Return"`

Defined in: [types/base.ts:140](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L140)

#### Inherited from

[`Return`](../namespaces/Core/interfaces/Return.md).[`kind`](../namespaces/Core/interfaces/Return.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:141](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L141)

#### Inherited from

[`BaseReturn`](../base/interfaces/BaseReturn.md).[`expr`](../base/interfaces/BaseReturn.md#expr-1)
