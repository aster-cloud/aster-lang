[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Case

Defined in: [types.ts:495](https://github.com/wontlost-ltd/aster-lang/blob/9ecf3b8a788316f1e50f83cdb3f5ac701018ca90/src/types.ts#L495)

Case 分支基础接口。

## Extends

- [`BaseCase`](../../../base/interfaces/BaseCase.md)\<[`Origin`](../../../interfaces/Origin.md), [`Pattern`](../type-aliases/Pattern.md), [`Return`](Return.md) \| [`Block`](Block.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/9ecf3b8a788316f1e50f83cdb3f5ac701018ca90/src/types/base.ts#L41)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`span`](../../../base/interfaces/BaseCase.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/9ecf3b8a788316f1e50f83cdb3f5ac701018ca90/src/types/base.ts#L42)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`origin`](../../../base/interfaces/BaseCase.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/9ecf3b8a788316f1e50f83cdb3f5ac701018ca90/src/types/base.ts#L43)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`file`](../../../base/interfaces/BaseCase.md#file)

***

### kind

> `readonly` **kind**: `"Case"`

Defined in: [types/base.ts:190](https://github.com/wontlost-ltd/aster-lang/blob/9ecf3b8a788316f1e50f83cdb3f5ac701018ca90/src/types/base.ts#L190)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`kind`](../../../base/interfaces/BaseCase.md#kind)

***

### pattern

> `readonly` **pattern**: [`Pattern`](../type-aliases/Pattern.md)

Defined in: [types/base.ts:191](https://github.com/wontlost-ltd/aster-lang/blob/9ecf3b8a788316f1e50f83cdb3f5ac701018ca90/src/types/base.ts#L191)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`pattern`](../../../base/interfaces/BaseCase.md#pattern)

***

### body

> `readonly` **body**: [`Block`](Block.md) \| [`Return`](Return.md)

Defined in: [types/base.ts:192](https://github.com/wontlost-ltd/aster-lang/blob/9ecf3b8a788316f1e50f83cdb3f5ac701018ca90/src/types/base.ts#L192)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`body`](../../../base/interfaces/BaseCase.md#body-1)
