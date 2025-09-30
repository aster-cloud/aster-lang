[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:249](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L249)

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

> `readonly` **kind**: `"Lambda"`

Defined in: [types.ts:250](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L250)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:251](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L251)

***

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:252](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L252)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types.ts:253](https://github.com/wontlost-ltd/aster-lang/blob/b07ed78be1381eb6c373a347f3c926ae125b2eb8/src/types.ts#L253)
