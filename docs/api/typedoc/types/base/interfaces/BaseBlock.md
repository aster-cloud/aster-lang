[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseBlock\<S, Stmt\>

Defined in: [types/base.ts:145](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types/base.ts#L145)

Block 基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Block`](../../interfaces/Block.md)
- [`Block`](../../namespaces/Core/interfaces/Block.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Stmt

`Stmt` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Block"`

Defined in: [types/base.ts:146](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types/base.ts#L146)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### statements

> `readonly` **statements**: readonly `Stmt`[]

Defined in: [types/base.ts:147](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types/base.ts#L147)
