[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseErr\<S, Expr\>

Defined in: [types/base.ts:358](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L358)

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

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Err"`

Defined in: [types/base.ts:359](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L359)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:360](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L360)
