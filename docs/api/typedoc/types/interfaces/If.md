[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: If

Defined in: [types.ts:199](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types.ts#L199)

If 语句基础接口。

## Extends

- [`BaseIf`](../base/interfaces/BaseIf.md)\<[`Span`](Span.md), [`Expression`](../type-aliases/Expression.md), [`Block`](Block.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:200](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types.ts#L200)

#### Overrides

[`BaseIf`](../base/interfaces/BaseIf.md).[`span`](../base/interfaces/BaseIf.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L42)

#### Inherited from

[`BaseIf`](../base/interfaces/BaseIf.md).[`origin`](../base/interfaces/BaseIf.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L43)

#### Inherited from

[`BaseIf`](../base/interfaces/BaseIf.md).[`file`](../base/interfaces/BaseIf.md#file)

***

### kind

> `readonly` **kind**: `"If"`

Defined in: [types/base.ts:171](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L171)

#### Inherited from

[`If`](../namespaces/Core/interfaces/If.md).[`kind`](../namespaces/Core/interfaces/If.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:172](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L172)

#### Inherited from

[`BaseIf`](../base/interfaces/BaseIf.md).[`cond`](../base/interfaces/BaseIf.md#cond)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types/base.ts:173](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L173)

#### Inherited from

[`BaseIf`](../base/interfaces/BaseIf.md).[`thenBlock`](../base/interfaces/BaseIf.md#thenblock)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types/base.ts:174](https://github.com/wontlost-ltd/aster-lang/blob/1691bae3248f4feb819a0747e3c0cb15f3704f67/src/types/base.ts#L174)

#### Inherited from

[`BaseIf`](../base/interfaces/BaseIf.md).[`elseBlock`](../base/interfaces/BaseIf.md#elseblock)
