[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypePii

Defined in: [types.ts:381](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types.ts#L381)

PII 类型标注（AST 层）
语法：@pii(L2, email) Text

## Extends

- [`AstNode`](../type-aliases/AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"TypePii"`

Defined in: [types.ts:382](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types.ts#L382)

#### Overrides

`AstNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:383](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types.ts#L383)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:384](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types.ts#L384)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:385](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types.ts#L385)

***

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:386](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types.ts#L386)

#### Overrides

`AstNode.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L42)

#### Inherited from

`AstNode.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L43)

#### Inherited from

`AstNode.file`
