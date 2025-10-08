[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypePii

Defined in: [types.ts:202](https://github.com/wontlost-ltd/aster-lang/blob/15ace69b6555ae75e14d12eefd375a82640f05b8/src/types.ts#L202)

PII 类型标注（AST 层）
语法：@pii(L2, email) Text

## Extends

- [`AstNode`](../type-aliases/AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"TypePii"`

Defined in: [types.ts:203](https://github.com/wontlost-ltd/aster-lang/blob/15ace69b6555ae75e14d12eefd375a82640f05b8/src/types.ts#L203)

#### Overrides

`AstNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:204](https://github.com/wontlost-ltd/aster-lang/blob/15ace69b6555ae75e14d12eefd375a82640f05b8/src/types.ts#L204)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:205](https://github.com/wontlost-ltd/aster-lang/blob/15ace69b6555ae75e14d12eefd375a82640f05b8/src/types.ts#L205)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:206](https://github.com/wontlost-ltd/aster-lang/blob/15ace69b6555ae75e14d12eefd375a82640f05b8/src/types.ts#L206)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/15ace69b6555ae75e14d12eefd375a82640f05b8/src/types/base.ts#L32)

#### Inherited from

`AstNode.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/15ace69b6555ae75e14d12eefd375a82640f05b8/src/types/base.ts#L33)

#### Inherited from

`AstNode.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/15ace69b6555ae75e14d12eefd375a82640f05b8/src/types/base.ts#L34)

#### Inherited from

`AstNode.file`
