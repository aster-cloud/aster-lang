[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Err

Defined in: [types.ts:395](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types.ts#L395)

Err 表达式基础接口。

## Extends

- [`BaseErr`](../../../base/interfaces/BaseErr.md)\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L32)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`span`](../../../base/interfaces/BaseErr.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L33)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`origin`](../../../base/interfaces/BaseErr.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L34)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`file`](../../../base/interfaces/BaseErr.md#file)

***

### kind

> `readonly` **kind**: `"Err"`

Defined in: [types/base.ts:347](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L347)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`kind`](../../../base/interfaces/BaseErr.md#kind)

***

### expr

> `readonly` **expr**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:348](https://github.com/wontlost-ltd/aster-lang/blob/4f740bf8b0b4001bef959ad42cb3287de3af0207/src/types/base.ts#L348)

#### Inherited from

[`BaseErr`](../../../base/interfaces/BaseErr.md).[`expr`](../../../base/interfaces/BaseErr.md#expr-1)
