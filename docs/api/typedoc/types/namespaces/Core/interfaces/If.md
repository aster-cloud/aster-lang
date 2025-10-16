[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: If

Defined in: [types.ts:335](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types.ts#L335)

If 语句基础接口。

## Extends

- [`BaseIf`](../../../base/interfaces/BaseIf.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md), [`Block`](Block.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L32)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`span`](../../../base/interfaces/BaseIf.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L33)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`origin`](../../../base/interfaces/BaseIf.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L34)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`file`](../../../base/interfaces/BaseIf.md#file)

***

### kind

> `readonly` **kind**: `"If"`

Defined in: [types/base.ts:162](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L162)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`kind`](../../../base/interfaces/BaseIf.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:163](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L163)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`cond`](../../../base/interfaces/BaseIf.md#cond)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types/base.ts:164](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L164)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`thenBlock`](../../../base/interfaces/BaseIf.md#thenblock)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types/base.ts:165](https://github.com/wontlost-ltd/aster-lang/blob/68924221b8533654469b888f5c3b57ebbcddb595/src/types/base.ts#L165)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`elseBlock`](../../../base/interfaces/BaseIf.md#elseblock)
