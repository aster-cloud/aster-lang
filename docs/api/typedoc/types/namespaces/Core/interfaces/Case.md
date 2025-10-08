[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Case

Defined in: [types.ts:271](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types.ts#L271)

Case 分支基础接口。

## Extends

- [`BaseCase`](../../../base/interfaces/BaseCase.md)\<[`Origin`](../../../interfaces/Origin.md), [`Pattern`](../type-aliases/Pattern.md), [`Return`](Return.md) \| [`Block`](Block.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L32)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`span`](../../../base/interfaces/BaseCase.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L33)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`origin`](../../../base/interfaces/BaseCase.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L34)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`file`](../../../base/interfaces/BaseCase.md#file)

***

### kind

> `readonly` **kind**: `"Case"`

Defined in: [types/base.ts:180](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L180)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`kind`](../../../base/interfaces/BaseCase.md#kind)

***

### pattern

> `readonly` **pattern**: [`Pattern`](../type-aliases/Pattern.md)

Defined in: [types/base.ts:181](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L181)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`pattern`](../../../base/interfaces/BaseCase.md#pattern)

***

### body

> `readonly` **body**: [`Block`](Block.md) \| [`Return`](Return.md)

Defined in: [types/base.ts:182](https://github.com/wontlost-ltd/aster-lang/blob/64b8d6f3b0968252be6476663ef660d7ac8c4546/src/types/base.ts#L182)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`body`](../../../base/interfaces/BaseCase.md#body-1)
