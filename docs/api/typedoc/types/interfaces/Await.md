[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Await

Defined in: [types.ts:209](https://github.com/wontlost-ltd/aster-lang/blob/182cd56309a0bb85c21ecd155862d52b481546d5/src/types.ts#L209)

Await 表达式基础接口。

## Extends

- [`BaseAwait`](../base/interfaces/BaseAwait.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/182cd56309a0bb85c21ecd155862d52b481546d5/src/types/base.ts#L32)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`span`](../base/interfaces/BaseAwait.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/182cd56309a0bb85c21ecd155862d52b481546d5/src/types/base.ts#L33)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`origin`](../base/interfaces/BaseAwait.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/182cd56309a0bb85c21ecd155862d52b481546d5/src/types/base.ts#L34)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`file`](../base/interfaces/BaseAwait.md#file)

***

### kind

> `readonly` **kind**: `"Await"`

Defined in: [types/base.ts:370](https://github.com/wontlost-ltd/aster-lang/blob/182cd56309a0bb85c21ecd155862d52b481546d5/src/types/base.ts#L370)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`kind`](../base/interfaces/BaseAwait.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:371](https://github.com/wontlost-ltd/aster-lang/blob/182cd56309a0bb85c21ecd155862d52b481546d5/src/types/base.ts#L371)

#### Inherited from

[`BaseAwait`](../base/interfaces/BaseAwait.md).[`expr`](../base/interfaces/BaseAwait.md#expr-1)
