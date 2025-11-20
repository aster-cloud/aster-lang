[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseErr\<S, Expr\>

Defined in: [types/base.ts:399](https://github.com/wontlost-ltd/aster-lang/blob/0d39658db4faf20f48f10b0c8fb7fe31023ff8e1/src/types/base.ts#L399)

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

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/0d39658db4faf20f48f10b0c8fb7fe31023ff8e1/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/0d39658db4faf20f48f10b0c8fb7fe31023ff8e1/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/0d39658db4faf20f48f10b0c8fb7fe31023ff8e1/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Err"`

Defined in: [types/base.ts:400](https://github.com/wontlost-ltd/aster-lang/blob/0d39658db4faf20f48f10b0c8fb7fe31023ff8e1/src/types/base.ts#L400)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:401](https://github.com/wontlost-ltd/aster-lang/blob/0d39658db4faf20f48f10b0c8fb7fe31023ff8e1/src/types/base.ts#L401)
