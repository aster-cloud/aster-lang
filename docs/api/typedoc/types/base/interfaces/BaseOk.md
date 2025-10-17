[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseOk\<S, Expr\>

Defined in: [types/base.ts:338](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L338)

Ok 表达式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Ok`](../../interfaces/Ok.md)
- [`Ok`](../../namespaces/Core/interfaces/Ok.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Ok"`

Defined in: [types/base.ts:339](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L339)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:340](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L340)
