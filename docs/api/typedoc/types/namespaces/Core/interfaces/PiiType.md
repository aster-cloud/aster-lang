[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: PiiType

Defined in: [types.ts:342](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types.ts#L342)

PII 类型（Core IR 层）
用于运行时 PII 数据流跟踪和污点分析

## Extends

- [`CoreNode`](../type-aliases/CoreNode.md)

## Properties

### kind

> `readonly` **kind**: `"PiiType"`

Defined in: [types.ts:343](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types.ts#L343)

#### Overrides

`CoreNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:344](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types.ts#L344)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../../../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:345](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types.ts#L345)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../../../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:346](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types.ts#L346)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L32)

#### Inherited from

`CoreNode.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L33)

#### Inherited from

`CoreNode.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/2ffe1d13bd22d37eb9544faeb630305db5f50636/src/types/base.ts#L34)

#### Inherited from

`CoreNode.file`
