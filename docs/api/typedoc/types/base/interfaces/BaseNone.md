[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseNone\<S\>

Defined in: [types/base.ts:415](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L415)

None 表达式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`None`](../../interfaces/None.md)
- [`None`](../../namespaces/Core/interfaces/None.md)

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

> `readonly` **kind**: `"None"`

Defined in: [types/base.ts:416](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L416)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)
