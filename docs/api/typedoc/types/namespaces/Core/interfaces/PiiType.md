[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: PiiType

Defined in: [types.ts:383](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types.ts#L383)

PII 类型（Core IR 层）
用于运行时 PII 数据流跟踪和污点分析

## Extends

- [`CoreNode`](../type-aliases/CoreNode.md)

## Properties

### kind

> `readonly` **kind**: `"PiiType"`

Defined in: [types.ts:384](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types.ts#L384)

#### Overrides

`CoreNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:385](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types.ts#L385)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../../../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:386](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types.ts#L386)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../../../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:387](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types.ts#L387)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types/base.ts#L32)

#### Inherited from

`CoreNode.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types/base.ts#L33)

#### Inherited from

`CoreNode.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/4c104476feb1dd10d1e24655952797af86444e2f/src/types/base.ts#L34)

#### Inherited from

`CoreNode.file`
