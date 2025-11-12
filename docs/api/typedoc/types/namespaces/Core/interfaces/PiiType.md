[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: PiiType

Defined in: [types.ts:566](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types.ts#L566)

PII 类型（Core IR 层）
用于运行时 PII 数据流跟踪和污点分析

## Extends

- [`CoreNode`](../type-aliases/CoreNode.md)

## Properties

### kind

> `readonly` **kind**: `"PiiType"`

Defined in: [types.ts:567](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types.ts#L567)

#### Overrides

`CoreNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:568](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types.ts#L568)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../../../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:569](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types.ts#L569)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../../../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:570](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types.ts#L570)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L41)

#### Inherited from

`CoreNode.span`

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L42)

#### Inherited from

`CoreNode.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/4868096da64d14fe24f9ace5ff431d987a4d0cc2/src/types/base.ts#L43)

#### Inherited from

`CoreNode.file`
