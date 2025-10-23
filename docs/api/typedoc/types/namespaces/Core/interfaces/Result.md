[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Result

Defined in: [types.ts:533](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L533)

Result 类型基础接口。

## Extends

- [`BaseResult`](../../../base/interfaces/BaseResult.md)\<[`Origin`](../../../interfaces/Origin.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L34)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`span`](../../../base/interfaces/BaseResult.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L35)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`origin`](../../../base/interfaces/BaseResult.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L36)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`file`](../../../base/interfaces/BaseResult.md#file)

***

### kind

> `readonly` **kind**: `"Result"`

Defined in: [types/base.ts:428](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L428)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`kind`](../../../base/interfaces/BaseResult.md#kind)

***

### ok

> `readonly` **ok**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:429](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L429)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`ok`](../../../base/interfaces/BaseResult.md#ok)

***

### err

> `readonly` **err**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:430](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L430)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`err`](../../../base/interfaces/BaseResult.md#err)
