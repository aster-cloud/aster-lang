[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: If

Defined in: [types.ts:441](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L441)

If 语句基础接口。

## Extends

- [`BaseIf`](../../../base/interfaces/BaseIf.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md), [`Block`](Block.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L34)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`span`](../../../base/interfaces/BaseIf.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L35)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`origin`](../../../base/interfaces/BaseIf.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L36)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`file`](../../../base/interfaces/BaseIf.md#file)

***

### kind

> `readonly` **kind**: `"If"`

Defined in: [types/base.ts:164](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L164)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`kind`](../../../base/interfaces/BaseIf.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:165](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L165)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`cond`](../../../base/interfaces/BaseIf.md#cond)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types/base.ts:166](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L166)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`thenBlock`](../../../base/interfaces/BaseIf.md#thenblock)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types/base.ts:167](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L167)

#### Inherited from

[`BaseIf`](../../../base/interfaces/BaseIf.md).[`elseBlock`](../../../base/interfaces/BaseIf.md#elseblock)
