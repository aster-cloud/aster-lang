[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseTypeVar\<S\>

Defined in: [types/base.ts:401](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L401)

类型变量基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`TypeVar`](../../interfaces/TypeVar.md)
- [`TypeVar`](../../namespaces/Core/interfaces/TypeVar.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"TypeVar"`

Defined in: [types/base.ts:402](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L402)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:403](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L403)
