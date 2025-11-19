[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseFuncType\<S, T\>

Defined in: [types/base.ts:501](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L501)

函数类型基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`FuncType`](../../interfaces/FuncType.md)
- [`FuncType`](../../namespaces/Core/interfaces/FuncType.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"FuncType"`

Defined in: [types/base.ts:502](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L502)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### params

> `readonly` **params**: readonly `T`[]

Defined in: [types/base.ts:503](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L503)

***

### ret

> `readonly` **ret**: `T`

Defined in: [types/base.ts:504](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L504)
