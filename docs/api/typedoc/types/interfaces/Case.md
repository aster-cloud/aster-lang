[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Case

Defined in: [types.ts:222](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types.ts#L222)

Case 分支基础接口。

## Extends

- [`BaseCase`](../base/interfaces/BaseCase.md)\<[`Span`](Span.md), [`Pattern`](../type-aliases/Pattern.md), [`Return`](Return.md) \| [`Block`](Block.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:223](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types.ts#L223)

#### Overrides

[`BaseCase`](../base/interfaces/BaseCase.md).[`span`](../base/interfaces/BaseCase.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L42)

#### Inherited from

[`BaseCase`](../base/interfaces/BaseCase.md).[`origin`](../base/interfaces/BaseCase.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L43)

#### Inherited from

[`BaseCase`](../base/interfaces/BaseCase.md).[`file`](../base/interfaces/BaseCase.md#file)

***

### kind

> `readonly` **kind**: `"Case"`

Defined in: [types/base.ts:190](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L190)

#### Inherited from

[`Case`](../namespaces/Core/interfaces/Case.md).[`kind`](../namespaces/Core/interfaces/Case.md#kind)

***

### pattern

> `readonly` **pattern**: [`Pattern`](../type-aliases/Pattern.md)

Defined in: [types/base.ts:191](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L191)

#### Inherited from

[`BaseCase`](../base/interfaces/BaseCase.md).[`pattern`](../base/interfaces/BaseCase.md#pattern)

***

### body

> `readonly` **body**: [`Block`](Block.md) \| [`Return`](Return.md)

Defined in: [types/base.ts:192](https://github.com/wontlost-ltd/aster-lang/blob/08894f0ec715ec098e0d6fadc4b448f4f075653b/src/types/base.ts#L192)

#### Inherited from

[`BaseCase`](../base/interfaces/BaseCase.md).[`body`](../base/interfaces/BaseCase.md#body-1)
