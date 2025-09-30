[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Result

Defined in: [types.ts:314](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L314)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### span?

> `readonly` `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:62](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L62)

#### Inherited from

[`AstNode`](AstNode.md).[`span`](AstNode.md#span)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types.ts:63](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L63)

#### Inherited from

[`AstNode`](AstNode.md).[`file`](AstNode.md#file)

***

### kind

> `readonly` **kind**: `"Result"`

Defined in: [types.ts:315](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L315)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### ok

> `readonly` **ok**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:316](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L316)

***

### err

> `readonly` **err**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:317](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L317)
