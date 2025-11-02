[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Result

Defined in: [types.ts:375](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types.ts#L375)

Result 类型基础接口。

## Extends

- [`BaseResult`](../base/interfaces/BaseResult.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:376](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types.ts#L376)

#### Overrides

[`BaseResult`](../base/interfaces/BaseResult.md).[`span`](../base/interfaces/BaseResult.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L42)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`origin`](../base/interfaces/BaseResult.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L43)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`file`](../base/interfaces/BaseResult.md#file)

***

### kind

> `readonly` **kind**: `"Result"`

Defined in: [types/base.ts:435](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L435)

#### Inherited from

[`Result`](../namespaces/Core/interfaces/Result.md).[`kind`](../namespaces/Core/interfaces/Result.md#kind)

***

### ok

> `readonly` **ok**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:436](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L436)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`ok`](../base/interfaces/BaseResult.md#ok)

***

### err

> `readonly` **err**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:437](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L437)

#### Inherited from

[`BaseResult`](../base/interfaces/BaseResult.md).[`err`](../base/interfaces/BaseResult.md#err)
