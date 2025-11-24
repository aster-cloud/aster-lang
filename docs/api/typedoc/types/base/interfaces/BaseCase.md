[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseCase\<S, Pat, Body\>

Defined in: [types/base.ts:189](https://github.com/wontlost-ltd/aster-lang/blob/0dcc7f727b18f3af07d94dceb461ca4e23de3261/src/types/base.ts#L189)

Case 分支基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Case`](../../interfaces/Case.md)
- [`Case`](../../namespaces/Core/interfaces/Case.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Pat

`Pat` = `unknown`

### Body

`Body` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/0dcc7f727b18f3af07d94dceb461ca4e23de3261/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/0dcc7f727b18f3af07d94dceb461ca4e23de3261/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/0dcc7f727b18f3af07d94dceb461ca4e23de3261/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Case"`

Defined in: [types/base.ts:190](https://github.com/wontlost-ltd/aster-lang/blob/0dcc7f727b18f3af07d94dceb461ca4e23de3261/src/types/base.ts#L190)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### pattern

> `readonly` **pattern**: `Pat`

Defined in: [types/base.ts:191](https://github.com/wontlost-ltd/aster-lang/blob/0dcc7f727b18f3af07d94dceb461ca4e23de3261/src/types/base.ts#L191)

***

### body

> `readonly` **body**: `Body`

Defined in: [types/base.ts:192](https://github.com/wontlost-ltd/aster-lang/blob/0dcc7f727b18f3af07d94dceb461ca4e23de3261/src/types/base.ts#L192)
