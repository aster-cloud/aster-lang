[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: If

Defined in: [types.ts:443](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types.ts#L443)

If 语句基础接口。

## Extends

- [`BaseIf`](../../../base/interfaces/BaseIf.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md), [`Block`](Block.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L41)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`span`](../../../base/interfaces/BaseIf.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L42)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`origin`](../../../base/interfaces/BaseIf.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L43)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`file`](../../../base/interfaces/BaseIf.md#file)

***

### kind

> `readonly` **kind**: `"If"`

Defined in: [types/base.ts:171](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L171)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`kind`](../../../base/interfaces/BaseIf.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:172](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L172)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`cond`](../../../base/interfaces/BaseIf.md#cond)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types/base.ts:173](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L173)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`thenBlock`](../../../base/interfaces/BaseIf.md#thenblock)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types/base.ts:174](https://github.com/wontlost-ltd/aster-lang/blob/ad77187a40dbdabfdba115d97c226df60c173499/src/types/base.ts#L174)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`elseBlock`](../../../base/interfaces/BaseIf.md#elseblock)
