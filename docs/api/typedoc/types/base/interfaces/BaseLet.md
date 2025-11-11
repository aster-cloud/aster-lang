[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseLet\<S, Expr\>

Defined in: [types/base.ts:128](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types/base.ts#L128)

Let 绑定基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Let`](../../interfaces/Let.md)
- [`Let`](../../namespaces/Core/interfaces/Let.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Let"`

Defined in: [types/base.ts:129](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types/base.ts#L129)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:130](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types/base.ts#L130)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:131](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types/base.ts#L131)
