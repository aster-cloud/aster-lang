[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Func

Defined in: [types.ts:376](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L376)

## Extends

- [`CoreNode`](CoreNode.md)

## Properties

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types.ts:344](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L344)

#### Inherited from

[`CoreNode`](CoreNode.md).[`origin`](CoreNode.md#origin)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types.ts:377](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L377)

#### Overrides

[`CoreNode`](CoreNode.md).[`kind`](CoreNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types.ts:378](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L378)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types.ts:379](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L379)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:380](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L380)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:381](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L381)

***

### effects

> `readonly` **effects**: readonly [`Effect`](../../../enumerations/Effect.md)[]

Defined in: [types.ts:382](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L382)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types.ts:383](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L383)

***

### effectCaps?

> `readonly` `optional` **effectCaps**: `object`

Defined in: [types.ts:384](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L384)

#### io?

> `readonly` `optional` **io**: readonly `string`[]
