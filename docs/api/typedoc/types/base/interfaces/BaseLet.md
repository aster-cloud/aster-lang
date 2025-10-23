[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseLet\<S, Expr\>

Defined in: [types/base.ts:121](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L121)

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

> `readonly` `optional` **span**: `HasFileProp`\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `HasFileProp`\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L35)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L36)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Let"`

Defined in: [types/base.ts:122](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L122)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:123](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L123)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:124](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L124)
