[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseAwait\<S, Expr\>

Defined in: [types/base.ts:421](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L421)

Await 表达式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Await`](../../interfaces/Await.md)
- [`Await`](../../namespaces/Core/interfaces/Await.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Await"`

Defined in: [types/base.ts:422](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L422)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:423](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L423)
