[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Func

Defined in: [types.ts:85](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L85)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types.ts:86](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L86)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types.ts:87](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L87)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types.ts:88](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L88)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:89](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L89)

***

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:90](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L90)

***

### effects

> `readonly` **effects**: readonly `string`[]

Defined in: [types.ts:91](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L91)

***

### body

> `readonly` **body**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:92](https://github.com/wontlost-ltd/aster-lang/blob/b3f4f92e2fab21136a15625be5d004d943480a0e/src/types.ts#L92)
