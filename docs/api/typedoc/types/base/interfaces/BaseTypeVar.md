[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseTypeVar\<S\>

Defined in: [types/base.ts:442](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L442)

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

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"TypeVar"`

Defined in: [types/base.ts:443](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L443)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:444](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L444)
