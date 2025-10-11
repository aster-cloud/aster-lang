[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseConstruct\<S, Field\>

Defined in: [types/base.ts:321](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L321)

构造器表达式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Construct`](../../interfaces/Construct.md)
- [`Construct`](../../namespaces/Core/interfaces/Construct.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Field

`Field` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Construct"`

Defined in: [types/base.ts:322](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L322)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### typeName

> `readonly` **typeName**: `string`

Defined in: [types/base.ts:323](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L323)

***

### fields

> `readonly` **fields**: readonly `Field`[]

Defined in: [types/base.ts:324](https://github.com/wontlost-ltd/aster-lang/blob/9a442c516389168ad783665c1531d6c31447ae23/src/types/base.ts#L324)
