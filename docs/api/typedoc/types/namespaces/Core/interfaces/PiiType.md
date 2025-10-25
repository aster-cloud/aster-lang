[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: PiiType

Defined in: [types.ts:517](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L517)

PII 类型（Core IR 层）
用于运行时 PII 数据流跟踪和污点分析

## Extends

- [`CoreNode`](../type-aliases/CoreNode.md)

## Properties

### kind

> `readonly` **kind**: `"PiiType"`

Defined in: [types.ts:518](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L518)

#### Overrides

`CoreNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:519](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L519)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../../../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:520](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L520)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../../../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:521](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types.ts#L521)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L34)

#### Inherited from

`CoreNode.span`

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L35)

#### Inherited from

`CoreNode.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L36)

#### Inherited from

`CoreNode.file`
