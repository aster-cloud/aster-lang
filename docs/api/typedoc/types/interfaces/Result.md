[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Result

Defined in: [types.ts:219](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types.ts#L219)

## Extends

- `BaseResult`\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types/base.ts#L32)

#### Inherited from

`Base.BaseResult.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types/base.ts#L33)

#### Inherited from

`Base.BaseResult.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types/base.ts#L34)

#### Inherited from

`Base.BaseResult.file`

***

### kind

> `readonly` **kind**: `"Result"`

Defined in: [types/base.ts:422](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types/base.ts#L422)

#### Inherited from

`Base.BaseResult.kind`

***

### ok

> `readonly` **ok**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:423](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types/base.ts#L423)

#### Inherited from

`Base.BaseResult.ok`

***

### err

> `readonly` **err**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:424](https://github.com/wontlost-ltd/aster-lang/blob/8bf329d1fd6e1197e3663ef108b86ba479c90b37/src/types/base.ts#L424)

#### Inherited from

`Base.BaseResult.err`
