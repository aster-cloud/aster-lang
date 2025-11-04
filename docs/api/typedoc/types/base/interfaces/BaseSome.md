[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseSome\<S, Expr\>

Defined in: [types/base.ts:366](https://github.com/wontlost-ltd/aster-lang/blob/7f1579c9ec9a5dcc01ae1bc900fe3bdcbd01dce9/src/types/base.ts#L366)

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

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/7f1579c9ec9a5dcc01ae1bc900fe3bdcbd01dce9/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/7f1579c9ec9a5dcc01ae1bc900fe3bdcbd01dce9/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/7f1579c9ec9a5dcc01ae1bc900fe3bdcbd01dce9/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Some"`

Defined in: [types/base.ts:367](https://github.com/wontlost-ltd/aster-lang/blob/7f1579c9ec9a5dcc01ae1bc900fe3bdcbd01dce9/src/types/base.ts#L367)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:368](https://github.com/wontlost-ltd/aster-lang/blob/7f1579c9ec9a5dcc01ae1bc900fe3bdcbd01dce9/src/types/base.ts#L368)
