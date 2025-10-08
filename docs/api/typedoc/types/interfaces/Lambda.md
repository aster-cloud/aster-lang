[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:159](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types.ts#L159)

## Extends

- `BaseLambda`\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md), [`Block`](Block.md)\>

## Properties

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:160](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types.ts#L160)

#### Overrides

`Base.BaseLambda.retType`

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types/base.ts#L32)

#### Inherited from

`Base.BaseLambda.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types/base.ts#L33)

#### Inherited from

`Base.BaseLambda.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types/base.ts#L34)

#### Inherited from

`Base.BaseLambda.file`

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types/base.ts:311](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types/base.ts#L311)

#### Inherited from

[`Lambda`](../namespaces/Core/interfaces/Lambda.md).[`kind`](../namespaces/Core/interfaces/Lambda.md#kind)

***

### params

> `readonly` **params**: readonly `BaseParameter`\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:312](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types/base.ts#L312)

#### Inherited from

`Base.BaseLambda.params`

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:314](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types/base.ts#L314)

#### Inherited from

`Base.BaseLambda.body`
