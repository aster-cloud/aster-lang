[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypePii

Defined in: [types.ts:246](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types.ts#L246)

PII 类型标注（AST 层）
语法：@pii(L2, email) Text

## Extends

- [`AstNode`](../type-aliases/AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"TypePii"`

Defined in: [types.ts:247](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types.ts#L247)

#### Overrides

`AstNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:248](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types.ts#L248)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:249](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types.ts#L249)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:250](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types.ts#L250)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types/base.ts#L32)

#### Inherited from

`AstNode.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types/base.ts#L33)

#### Inherited from

`AstNode.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/ecb135a7d7668ee5a34b1d09cf742b5655b7829c/src/types/base.ts#L34)

#### Inherited from

`AstNode.file`
