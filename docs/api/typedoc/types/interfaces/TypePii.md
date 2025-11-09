[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypePii

Defined in: [types.ts:372](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types.ts#L372)

PII 类型标注（AST 层）
语法：@pii(L2, email) Text

## Extends

- [`AstNode`](../type-aliases/AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"TypePii"`

Defined in: [types.ts:373](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types.ts#L373)

#### Overrides

`AstNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:374](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types.ts#L374)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:375](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types.ts#L375)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:376](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types.ts#L376)

***

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:377](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types.ts#L377)

#### Overrides

`AstNode.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L42)

#### Inherited from

`AstNode.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/8b40c0ffcba44ce641461ada8bbd5fcd27cd1f64/src/types/base.ts#L43)

#### Inherited from

`AstNode.file`
