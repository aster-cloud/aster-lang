[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseSet\<S, Expr\>

Defined in: [types/base.ts:130](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L130)

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

> `readonly` `optional` **span**: `HasFileProp`\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `HasFileProp`\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L35)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L36)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Set"`

Defined in: [types/base.ts:131](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L131)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:132](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L132)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:133](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L133)
