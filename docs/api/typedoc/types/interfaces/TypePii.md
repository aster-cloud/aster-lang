[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypePii

Defined in: [types.ts:345](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L345)

PII 类型标注（AST 层）
语法：@pii(L2, email) Text

## Extends

- [`AstNode`](../type-aliases/AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"TypePii"`

Defined in: [types.ts:346](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L346)

#### Overrides

`AstNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:347](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L347)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:348](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L348)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:349](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L349)

***

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:350](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L350)

#### Overrides

`AstNode.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L35)

#### Inherited from

`AstNode.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L36)

#### Inherited from

`AstNode.file`
