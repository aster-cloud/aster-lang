[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseReturn\<S, Expr\>

Defined in: [types/base.ts:137](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L137)

Return 语句基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Return`](../../interfaces/Return.md)
- [`Return`](../../namespaces/Core/interfaces/Return.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Return"`

Defined in: [types/base.ts:138](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L138)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:139](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/types/base.ts#L139)
