[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Lambda

Defined in: [types.ts:524](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L524)

## Extends

- [`CoreNode`](CoreNode.md)

## Properties

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types.ts:341](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L341)

#### Inherited from

[`CoreNode`](CoreNode.md).[`origin`](CoreNode.md#origin)

***

### kind

> `readonly` **kind**: `"Lambda"`

Defined in: [types.ts:525](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L525)

#### Overrides

[`CoreNode`](CoreNode.md).[`kind`](CoreNode.md#kind)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:526](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L526)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:527](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L527)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types.ts:528](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L528)

***

### captures?

> `readonly` `optional` **captures**: readonly `string`[]

Defined in: [types.ts:529](https://github.com/wontlost-ltd/aster-lang/blob/f4835243b7407eea7faa5aab94a9792f719aa9d7/src/types.ts#L529)
