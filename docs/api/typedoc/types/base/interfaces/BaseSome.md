[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseSome\<S, Expr\>

Defined in: [types/base.ts:353](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/types/base.ts#L353)

Some 表达式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Some`](../../interfaces/Some.md)
- [`Some`](../../namespaces/Core/interfaces/Some.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Some"`

Defined in: [types/base.ts:354](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/types/base.ts#L354)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:355](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/types/base.ts#L355)
