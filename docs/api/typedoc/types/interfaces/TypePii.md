[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypePii

Defined in: [types.ts:243](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types.ts#L243)

PII 类型标注（AST 层）
语法：@pii(L2, email) Text

## Extends

- [`AstNode`](../type-aliases/AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"TypePii"`

Defined in: [types.ts:244](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types.ts#L244)

#### Overrides

`AstNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:245](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types.ts#L245)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:246](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types.ts#L246)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:247](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types.ts#L247)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types/base.ts#L32)

#### Inherited from

`AstNode.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types/base.ts#L33)

#### Inherited from

`AstNode.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/d058d9c7dd4806e1a5ad4bae99abf86063fa8371/src/types/base.ts#L34)

#### Inherited from

`AstNode.file`
