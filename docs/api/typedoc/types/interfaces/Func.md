[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Func

Defined in: [types.ts:83](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L83)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types.ts:84](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L84)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types.ts:85](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L85)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types.ts:86](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L86)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:87](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L87)

***

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:88](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L88)

***

### effects

> `readonly` **effects**: readonly `string`[]

Defined in: [types.ts:89](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L89)

***

### body

> `readonly` **body**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:90](https://github.com/wontlost-ltd/aster-lang/blob/c2afec81f1ccfc11a50452409ee9aa69265cf04f/src/types.ts#L90)
