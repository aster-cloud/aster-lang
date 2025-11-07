[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseCall\<S, Expr\>

Defined in: [types/base.ts:314](https://github.com/wontlost-ltd/aster-lang/blob/fa50dbc54c51a80ed2059bde2d89a41958e445b2/src/types/base.ts#L314)

函数调用基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Call`](../../interfaces/Call.md)
- [`Call`](../../namespaces/Core/interfaces/Call.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/fa50dbc54c51a80ed2059bde2d89a41958e445b2/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/fa50dbc54c51a80ed2059bde2d89a41958e445b2/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/fa50dbc54c51a80ed2059bde2d89a41958e445b2/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Call"`

Defined in: [types/base.ts:315](https://github.com/wontlost-ltd/aster-lang/blob/fa50dbc54c51a80ed2059bde2d89a41958e445b2/src/types/base.ts#L315)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### target

> `readonly` **target**: `Expr`

Defined in: [types/base.ts:316](https://github.com/wontlost-ltd/aster-lang/blob/fa50dbc54c51a80ed2059bde2d89a41958e445b2/src/types/base.ts#L316)

***

### args

> `readonly` **args**: readonly `Expr`[]

Defined in: [types/base.ts:317](https://github.com/wontlost-ltd/aster-lang/blob/fa50dbc54c51a80ed2059bde2d89a41958e445b2/src/types/base.ts#L317)
