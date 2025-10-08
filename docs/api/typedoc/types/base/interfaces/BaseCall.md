[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseCall\<S, Expr\>

Defined in: [types/base.ts:301](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L301)

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

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Call"`

Defined in: [types/base.ts:302](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L302)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### target

> `readonly` **target**: `Expr`

Defined in: [types/base.ts:303](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L303)

***

### args

> `readonly` **args**: readonly `Expr`[]

Defined in: [types/base.ts:304](https://github.com/wontlost-ltd/aster-lang/blob/890200b8635ab0860bf1f19ed73954e8649bba2a/src/types/base.ts#L304)
