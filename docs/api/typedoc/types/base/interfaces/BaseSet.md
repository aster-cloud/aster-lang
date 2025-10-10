[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseSet\<S, Expr\>

Defined in: [types/base.ts:128](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L128)

Set 赋值基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Set`](../../interfaces/Set.md)
- [`Set`](../../namespaces/Core/interfaces/Set.md)

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

> `readonly` **kind**: `"Set"`

Defined in: [types/base.ts:129](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L129)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:130](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L130)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:131](https://github.com/wontlost-ltd/aster-lang/blob/44608e5a094893bf06e67cc4c9ca2750d8578ae7/src/types/base.ts#L131)
