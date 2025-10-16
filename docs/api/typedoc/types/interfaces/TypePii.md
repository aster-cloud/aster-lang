[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypePii

Defined in: [types.ts:270](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types.ts#L270)

PII 类型标注（AST 层）
语法：@pii(L2, email) Text

## Extends

- [`AstNode`](../type-aliases/AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"TypePii"`

Defined in: [types.ts:271](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types.ts#L271)

#### Overrides

`AstNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:272](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types.ts#L272)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:273](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types.ts#L273)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:274](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types.ts#L274)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L32)

#### Inherited from

`AstNode.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L33)

#### Inherited from

`AstNode.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L34)

#### Inherited from

`AstNode.file`
