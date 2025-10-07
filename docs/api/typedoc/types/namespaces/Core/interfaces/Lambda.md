[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:528](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L528)

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

> `readonly` **kind**: `"Lambda"`

Defined in: [types.ts:529](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L529)

#### Overrides

[`CoreNode`](CoreNode.md).[`kind`](CoreNode.md#kind)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:530](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L530)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:531](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L531)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types.ts:532](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L532)

***

### captures?

> `readonly` `optional` **captures**: readonly `string`[]

Defined in: [types.ts:533](https://github.com/wontlost-ltd/aster-lang/blob/026c79a6dbb38388284062b92af40be318ffbb13/src/types.ts#L533)
