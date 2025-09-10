[**aster-lang**](../../README.md)

***

# Interface: Func

Defined in: types.ts:79

## Extends

- [`AstNode`](AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"Func"`

Defined in: types.ts:80

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: types.ts:81

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: types.ts:82

***

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: types.ts:83

***

### effects

> `readonly` **effects**: readonly `string`[]

Defined in: types.ts:84

***

### body

> `readonly` **body**: `null` \| [`Block`](Block.md)

Defined in: types.ts:85
