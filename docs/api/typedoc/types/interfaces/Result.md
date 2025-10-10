[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Result

Defined in: [types.ts:260](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types.ts#L260)

Result 类型基础接口。

## Extends

- [`BaseResult`](../base/interfaces/BaseResult.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L32)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`span`](../base/interfaces/BaseResult.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L33)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`origin`](../base/interfaces/BaseResult.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L34)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`file`](../base/interfaces/BaseResult.md#file)

***

### kind

> `readonly` **kind**: `"Result"`

Defined in: [types/base.ts:423](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L423)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`kind`](../base/interfaces/BaseResult.md#kind)

***

### ok

> `readonly` **ok**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:424](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L424)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`ok`](../base/interfaces/BaseResult.md#ok)

***

### err

> `readonly` **err**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:425](https://github.com/wontlost-ltd/aster-lang/blob/7ffb3deb1dde9d3c5b570d525974d66eda9f80cb/src/types/base.ts#L425)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`err`](../base/interfaces/BaseResult.md#err)
