[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: PiiType

Defined in: [types.ts:577](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L577)

PII 类型（Core IR 层）
用于运行时 PII 数据流跟踪和污点分析

## Extends

- [`CoreNode`](../type-aliases/CoreNode.md)

## Properties

### kind

> `readonly` **kind**: `"PiiType"`

Defined in: [types.ts:578](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L578)

#### Overrides

`CoreNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:579](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L579)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../../../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:580](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L580)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../../../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:581](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L581)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L41)

#### Inherited from

`CoreNode.span`

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L42)

#### Inherited from

`CoreNode.origin`

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L43)

#### Inherited from

`CoreNode.file`
