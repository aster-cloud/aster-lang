[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseCall\<S, Expr\>

Defined in: [types/base.ts:302](https://github.com/wontlost-ltd/aster-lang/blob/182cd56309a0bb85c21ecd155862d52b481546d5/src/types/base.ts#L302)

函数调用基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Call`](../../interfaces/Call.md)
- [`Call`](../../namespaces/Core/interfaces/Call.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/182cd56309a0bb85c21ecd155862d52b481546d5/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/182cd56309a0bb85c21ecd155862d52b481546d5/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/182cd56309a0bb85c21ecd155862d52b481546d5/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Call"`

Defined in: [types/base.ts:303](https://github.com/wontlost-ltd/aster-lang/blob/182cd56309a0bb85c21ecd155862d52b481546d5/src/types/base.ts#L303)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### target

> `readonly` **target**: `Expr`

Defined in: [types/base.ts:304](https://github.com/wontlost-ltd/aster-lang/blob/182cd56309a0bb85c21ecd155862d52b481546d5/src/types/base.ts#L304)

***

### args

> `readonly` **args**: readonly `Expr`[]

Defined in: [types/base.ts:305](https://github.com/wontlost-ltd/aster-lang/blob/182cd56309a0bb85c21ecd155862d52b481546d5/src/types/base.ts#L305)
