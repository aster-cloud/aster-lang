[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseFuncType\<S, T\>

Defined in: [types/base.ts:453](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L453)

函数类型基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`FuncType`](../../interfaces/FuncType.md)
- [`FuncType`](../../namespaces/Core/interfaces/FuncType.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

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

> `readonly` **kind**: `"FuncType"`

Defined in: [types/base.ts:454](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L454)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### params

> `readonly` **params**: readonly `T`[]

Defined in: [types/base.ts:455](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L455)

***

### ret

> `readonly` **ret**: `T`

Defined in: [types/base.ts:456](https://github.com/wontlost-ltd/aster-lang/blob/7044bd6c549b9ab67789410d73995edfebc62745/src/types/base.ts#L456)
