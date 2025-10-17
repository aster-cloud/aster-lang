[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: PiiType

Defined in: [types.ts:434](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L434)

PII 类型（Core IR 层）
用于运行时 PII 数据流跟踪和污点分析

## Extends

- [`CoreNode`](../type-aliases/CoreNode.md)

## Properties

### kind

> `readonly` **kind**: `"PiiType"`

Defined in: [types.ts:435](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L435)

#### Overrides

`CoreNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:436](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L436)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../../../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:437](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L437)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../../../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:438](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L438)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L32)

#### Inherited from

`CoreNode.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L33)

#### Inherited from

`CoreNode.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types/base.ts#L34)

#### Inherited from

`CoreNode.file`
