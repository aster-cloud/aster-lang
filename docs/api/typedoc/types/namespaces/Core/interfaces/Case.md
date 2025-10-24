[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Case

Defined in: [types.ts:445](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L445)

Case 分支基础接口。

## Extends

- [`BaseCase`](../../../base/interfaces/BaseCase.md)\<[`Origin`](../../../interfaces/Origin.md), [`Pattern`](../type-aliases/Pattern.md), [`Return`](Return.md) \| [`Block`](Block.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L34)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`span`](../../../base/interfaces/BaseCase.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L35)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`origin`](../../../base/interfaces/BaseCase.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L36)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`file`](../../../base/interfaces/BaseCase.md#file)

***

### kind

> `readonly` **kind**: `"Case"`

Defined in: [types/base.ts:183](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L183)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`kind`](../../../base/interfaces/BaseCase.md#kind)

***

### pattern

> `readonly` **pattern**: [`Pattern`](../type-aliases/Pattern.md)

Defined in: [types/base.ts:184](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L184)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`pattern`](../../../base/interfaces/BaseCase.md#pattern)

***

### body

> `readonly` **body**: [`Block`](Block.md) \| [`Return`](Return.md)

Defined in: [types/base.ts:185](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L185)

#### Inherited from

[`BaseCase`](../../../base/interfaces/BaseCase.md).[`body`](../../../base/interfaces/BaseCase.md#body-1)
