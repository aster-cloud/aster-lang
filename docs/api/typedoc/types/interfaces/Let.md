[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Let

Defined in: [types.ts:199](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types.ts#L199)

Let 绑定基础接口。

## Extends

- [`BaseLet`](../base/interfaces/BaseLet.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:200](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types.ts#L200)

#### Overrides

[`BaseLet`](../base/interfaces/BaseLet.md).[`span`](../base/interfaces/BaseLet.md#span)

***

### nameSpan?

> `readonly` `optional` **nameSpan**: [`Span`](Span.md)

Defined in: [types.ts:201](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types.ts#L201)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L42)

#### Inherited from

[`BaseLet`](../base/interfaces/BaseLet.md).[`origin`](../base/interfaces/BaseLet.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L43)

#### Inherited from

[`BaseLet`](../base/interfaces/BaseLet.md).[`file`](../base/interfaces/BaseLet.md#file)

***

### kind

> `readonly` **kind**: `"Let"`

Defined in: [types/base.ts:129](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L129)

#### Inherited from

[`Let`](../namespaces/Core/interfaces/Let.md).[`kind`](../namespaces/Core/interfaces/Let.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:130](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L130)

#### Inherited from

[`Let`](../namespaces/Core/interfaces/Let.md).[`name`](../namespaces/Core/interfaces/Let.md#name)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:131](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L131)

#### Inherited from

[`BaseLet`](../base/interfaces/BaseLet.md).[`expr`](../base/interfaces/BaseLet.md#expr-1)
