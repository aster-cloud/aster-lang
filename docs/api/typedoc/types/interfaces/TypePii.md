[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypePii

Defined in: [types.ts:381](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types.ts#L381)

PII 类型标注（AST 层）
语法：@pii(L2, email) Text

## Extends

- [`AstNode`](../type-aliases/AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"TypePii"`

Defined in: [types.ts:382](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types.ts#L382)

#### Overrides

`AstNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:383](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types.ts#L383)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:384](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types.ts#L384)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:385](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types.ts#L385)

***

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:386](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types.ts#L386)

#### Overrides

`AstNode.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L42)

#### Inherited from

`AstNode.origin`

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/0c307273fd09aa4d31968abdaff80950a9db9b7f/src/types/base.ts#L43)

#### Inherited from

`AstNode.file`
