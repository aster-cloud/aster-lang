[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseMatch\<S, Expr, Case\>

Defined in: [types/base.ts:171](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L171)

Match 语句基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Match`](../../interfaces/Match.md)
- [`Match`](../../namespaces/Core/interfaces/Match.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

### Case

`Case` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: `S` *extends* [`Origin`](../../interfaces/Origin.md) ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L32)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `S` *extends* [`Span`](../../interfaces/Span.md) ? `never` : [`Origin`](../../interfaces/Origin.md)

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L33)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Match"`

Defined in: [types/base.ts:172](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L172)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:173](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L173)

***

### cases

> `readonly` **cases**: readonly `Case`[]

Defined in: [types/base.ts:174](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/types/base.ts#L174)
