[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseLet\<S, Expr\>

Defined in: [types/base.ts:119](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L119)

Let 绑定基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Let`](../../interfaces/Let.md)
- [`Let`](../../namespaces/Core/interfaces/Let.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Let"`

Defined in: [types/base.ts:120](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L120)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:121](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L121)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:122](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L122)
