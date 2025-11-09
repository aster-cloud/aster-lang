[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseReturn\<S, Expr\>

Defined in: [types/base.ts:146](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L146)

Return 语句基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Return`](../../interfaces/Return.md)
- [`Return`](../../namespaces/Core/interfaces/Return.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Return"`

Defined in: [types/base.ts:147](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L147)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:148](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L148)
