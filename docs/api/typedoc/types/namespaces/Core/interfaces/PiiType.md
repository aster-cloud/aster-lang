[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: PiiType

Defined in: [types.ts:610](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types.ts#L610)

PII 类型（Core IR 层）
用于运行时 PII 数据流跟踪和污点分析

## Extends

- [`CoreNode`](../type-aliases/CoreNode.md)

## Properties

### kind

> `readonly` **kind**: `"PiiType"`

Defined in: [types.ts:611](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types.ts#L611)

#### Overrides

`CoreNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:612](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types.ts#L612)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../../../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:613](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types.ts#L613)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../../../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:614](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types.ts#L614)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L41)

#### Inherited from

`CoreNode.span`

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L42)

#### Inherited from

`CoreNode.origin`

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/fd95cfef5bdde57ff61442c12f0f7eb090f06322/src/types/base.ts#L43)

#### Inherited from

`CoreNode.file`
