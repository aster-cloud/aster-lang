[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseErr\<S, Expr\>

Defined in: [types/base.ts:346](https://github.com/wontlost-ltd/aster-lang/blob/424562fcc358f728754d52c653dd622346cf0157/src/types/base.ts#L346)

Err 表达式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Err`](../../interfaces/Err.md)
- [`Err`](../../namespaces/Core/interfaces/Err.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/424562fcc358f728754d52c653dd622346cf0157/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/424562fcc358f728754d52c653dd622346cf0157/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/424562fcc358f728754d52c653dd622346cf0157/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Err"`

Defined in: [types/base.ts:347](https://github.com/wontlost-ltd/aster-lang/blob/424562fcc358f728754d52c653dd622346cf0157/src/types/base.ts#L347)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:348](https://github.com/wontlost-ltd/aster-lang/blob/424562fcc358f728754d52c653dd622346cf0157/src/types/base.ts#L348)
