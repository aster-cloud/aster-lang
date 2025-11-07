[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: FuncType

Defined in: [types.ts:387](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types.ts#L387)

函数类型基础接口。

## Extends

- [`BaseFuncType`](../base/interfaces/BaseFuncType.md)\<[`Span`](Span.md), [`Type`](../type-aliases/Type.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:388](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types.ts#L388)

#### Overrides

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`span`](../base/interfaces/BaseFuncType.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types/base.ts#L42)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`origin`](../base/interfaces/BaseFuncType.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types/base.ts#L43)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`file`](../base/interfaces/BaseFuncType.md#file)

***

### kind

> `readonly` **kind**: `"FuncType"`

Defined in: [types/base.ts:461](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types/base.ts#L461)

#### Inherited from

[`FuncType`](../namespaces/Core/interfaces/FuncType.md).[`kind`](../namespaces/Core/interfaces/FuncType.md#kind)

***

### params

> `readonly` **params**: readonly [`Type`](../type-aliases/Type.md)[]

Defined in: [types/base.ts:462](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types/base.ts#L462)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`params`](../base/interfaces/BaseFuncType.md#params)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:463](https://github.com/wontlost-ltd/aster-lang/blob/caebb607334e35d2baba538510f4ad8190311413/src/types/base.ts#L463)

#### Inherited from

[`BaseFuncType`](../base/interfaces/BaseFuncType.md).[`ret`](../base/interfaces/BaseFuncType.md#ret)
