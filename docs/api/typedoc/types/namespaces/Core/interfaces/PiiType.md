[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: PiiType

Defined in: [types.ts:387](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types.ts#L387)

PII 类型（Core IR 层）
用于运行时 PII 数据流跟踪和污点分析

## Extends

- [`CoreNode`](../type-aliases/CoreNode.md)

## Properties

### kind

> `readonly` **kind**: `"PiiType"`

Defined in: [types.ts:388](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types.ts#L388)

#### Overrides

`CoreNode.kind`

***

### baseType

> `readonly` **baseType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:389](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types.ts#L389)

***

### sensitivity

> `readonly` **sensitivity**: [`PiiSensitivityLevel`](../../../type-aliases/PiiSensitivityLevel.md)

Defined in: [types.ts:390](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types.ts#L390)

***

### category

> `readonly` **category**: [`PiiDataCategory`](../../../type-aliases/PiiDataCategory.md)

Defined in: [types.ts:391](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types.ts#L391)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L32)

#### Inherited from

`CoreNode.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L33)

#### Inherited from

`CoreNode.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/029e5ccdea6de45aac2bfe38dc2261cf00efc7a6/src/types/base.ts#L34)

#### Inherited from

`CoreNode.file`
