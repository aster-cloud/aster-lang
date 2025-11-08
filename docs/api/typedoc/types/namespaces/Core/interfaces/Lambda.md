[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:492](https://github.com/wontlost-ltd/aster-lang/blob/f1b05539f0a45448fa7c96fcf895807e73b4c6f5/src/types.ts#L492)

Lambda 表达式基础接口。

## Extends

- [`BaseLambda`](../../../base/interfaces/BaseLambda.md)\<[`Origin`](../../../interfaces/Origin.md), [`Type`](../type-aliases/Type.md), [`Block`](Block.md)\>

## Properties

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:493](https://github.com/wontlost-ltd/aster-lang/blob/f1b05539f0a45448fa7c96fcf895807e73b4c6f5/src/types.ts#L493)

***

### captures?

> `readonly` `optional` **captures**: readonly `string`[]

Defined in: [types.ts:494](https://github.com/wontlost-ltd/aster-lang/blob/f1b05539f0a45448fa7c96fcf895807e73b4c6f5/src/types.ts#L494)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/f1b05539f0a45448fa7c96fcf895807e73b4c6f5/src/types/base.ts#L41)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`span`](../../../base/interfaces/BaseLambda.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/f1b05539f0a45448fa7c96fcf895807e73b4c6f5/src/types/base.ts#L42)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`origin`](../../../base/interfaces/BaseLambda.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/f1b05539f0a45448fa7c96fcf895807e73b4c6f5/src/types/base.ts#L43)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`file`](../../../base/interfaces/BaseLambda.md#file)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types/base.ts:324](https://github.com/wontlost-ltd/aster-lang/blob/f1b05539f0a45448fa7c96fcf895807e73b4c6f5/src/types/base.ts#L324)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`kind`](../../../base/interfaces/BaseLambda.md#kind)

***

### params

> `readonly` **params**: readonly [`BaseParameter`](../../../base/interfaces/BaseParameter.md)\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:325](https://github.com/wontlost-ltd/aster-lang/blob/f1b05539f0a45448fa7c96fcf895807e73b4c6f5/src/types/base.ts#L325)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`params`](../../../base/interfaces/BaseLambda.md#params)

***

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types/base.ts:326](https://github.com/wontlost-ltd/aster-lang/blob/f1b05539f0a45448fa7c96fcf895807e73b4c6f5/src/types/base.ts#L326)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`retType`](../../../base/interfaces/BaseLambda.md#rettype)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types/base.ts:327](https://github.com/wontlost-ltd/aster-lang/blob/f1b05539f0a45448fa7c96fcf895807e73b4c6f5/src/types/base.ts#L327)

#### Inherited from

[`BaseLambda`](../../../base/interfaces/BaseLambda.md).[`body`](../../../base/interfaces/BaseLambda.md#body)
