[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseSome\<S, Expr\>

Defined in: [types/base.ts:359](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L359)

Some 表达式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Some`](../../interfaces/Some.md)
- [`Some`](../../namespaces/Core/interfaces/Some.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `HasFileProp`\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `HasFileProp`\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L35)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L36)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Some"`

Defined in: [types/base.ts:360](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L360)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:361](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L361)
