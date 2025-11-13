[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Result

Defined in: [types.ts:583](https://github.com/wontlost-ltd/aster-lang/blob/098e8b967ef568d4f69ccd6202c35e3fb6d0bfd2/src/types.ts#L583)

Result 类型基础接口。

## Extends

- [`BaseResult`](../../../base/interfaces/BaseResult.md)\<[`Origin`](../../../interfaces/Origin.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/098e8b967ef568d4f69ccd6202c35e3fb6d0bfd2/src/types/base.ts#L41)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`span`](../../../base/interfaces/BaseResult.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/098e8b967ef568d4f69ccd6202c35e3fb6d0bfd2/src/types/base.ts#L42)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`origin`](../../../base/interfaces/BaseResult.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/098e8b967ef568d4f69ccd6202c35e3fb6d0bfd2/src/types/base.ts#L43)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`file`](../../../base/interfaces/BaseResult.md#file)

***

### kind

> `readonly` **kind**: `"Result"`

Defined in: [types/base.ts:476](https://github.com/wontlost-ltd/aster-lang/blob/098e8b967ef568d4f69ccd6202c35e3fb6d0bfd2/src/types/base.ts#L476)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`kind`](../../../base/interfaces/BaseResult.md#kind)

***

### ok

> `readonly` **ok**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:477](https://github.com/wontlost-ltd/aster-lang/blob/098e8b967ef568d4f69ccd6202c35e3fb6d0bfd2/src/types/base.ts#L477)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`ok`](../../../base/interfaces/BaseResult.md#ok)

***

### err

> `readonly` **err**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:478](https://github.com/wontlost-ltd/aster-lang/blob/098e8b967ef568d4f69ccd6202c35e3fb6d0bfd2/src/types/base.ts#L478)

#### Inherited from

[`BaseResult`](../../../base/interfaces/BaseResult.md).[`err`](../../../base/interfaces/BaseResult.md#err)
