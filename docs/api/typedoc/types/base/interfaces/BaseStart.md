[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseStart\<S, Expr\>

Defined in: [types/base.ts:198](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L198)

Start 异步任务基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Start`](../../interfaces/Start.md)
- [`Start`](../../namespaces/Core/interfaces/Start.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Start"`

Defined in: [types/base.ts:199](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L199)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:200](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L200)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:201](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L201)
