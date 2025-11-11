[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: FuncType

Defined in: [types.ts:413](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types.ts#L413)

函数类型基础接口。

## Extends

- [`BaseFuncType`](../base/interfaces/BaseFuncType.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:414](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types.ts#L414)

#### Overrides

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`span`](../base/interfaces/BaseFuncType.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L42)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`origin`](../base/interfaces/BaseFuncType.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L43)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`file`](../base/interfaces/BaseFuncType.md#file)

***

### kind

> `readonly` **kind**: `"FuncType"`

Defined in: [types/base.ts:502](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L502)

#### Inherited from

[`FuncType`](../namespaces/Core/interfaces/FuncType.md).[`kind`](../namespaces/Core/interfaces/FuncType.md#kind)

***

### params

> `readonly` **params**: readonly [`Type`](../type-aliases/Type.md)[]

Defined in: [types/base.ts:503](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L503)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`params`](../base/interfaces/BaseFuncType.md#params)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:504](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L504)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`ret`](../base/interfaces/BaseFuncType.md#ret)
