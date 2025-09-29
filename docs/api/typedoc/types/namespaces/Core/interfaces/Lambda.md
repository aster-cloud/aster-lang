[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:523](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L523)

## Extends

- [`CoreNode`](CoreNode.md)

## Properties

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types.ts:340](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L340)

#### Inherited from

[`CoreNode`](CoreNode.md).[`origin`](CoreNode.md#origin)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types.ts:524](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L524)

#### Overrides

[`CoreNode`](CoreNode.md).[`kind`](CoreNode.md#kind)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:525](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L525)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:526](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L526)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types.ts:527](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L527)

***

### captures?

> `readonly` `optional` **captures**: readonly `string`[]

Defined in: [types.ts:528](https://github.com/wontlost-ltd/aster-lang/blob/b4a8510b31e37921c06a9d1132fe8c41a3d335af/src/types.ts#L528)
