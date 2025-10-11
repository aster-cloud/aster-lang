[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseFuncType\<S, T\>

Defined in: [types/base.ts:448](https://github.com/wontlost-ltd/aster-lang/blob/424562fcc358f728754d52c653dd622346cf0157/src/types/base.ts#L448)

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

> `readonly` **kind**: `"FuncType"`

Defined in: [types/base.ts:449](https://github.com/wontlost-ltd/aster-lang/blob/424562fcc358f728754d52c653dd622346cf0157/src/types/base.ts#L449)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### params

> `readonly` **params**: readonly `T`[]

Defined in: [types/base.ts:450](https://github.com/wontlost-ltd/aster-lang/blob/424562fcc358f728754d52c653dd622346cf0157/src/types/base.ts#L450)

***

### ret

> `readonly` **ret**: `T`

Defined in: [types/base.ts:451](https://github.com/wontlost-ltd/aster-lang/blob/424562fcc358f728754d52c653dd622346cf0157/src/types/base.ts#L451)
