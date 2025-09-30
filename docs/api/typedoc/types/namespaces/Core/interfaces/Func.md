[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Func

Defined in: [types.ts:373](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L373)

## Extends

- [`CoreNode`](CoreNode.md)

## Properties

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types.ts:341](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L341)

#### Inherited from

[`CoreNode`](CoreNode.md).[`origin`](CoreNode.md#origin)

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types.ts:374](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L374)

#### Overrides

[`CoreNode`](CoreNode.md).[`kind`](CoreNode.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types.ts:375](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L375)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types.ts:376](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L376)

***

### params

> `readonly` **params**: readonly [`Parameter`](Parameter.md)[]

Defined in: [types.ts:377](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L377)

***

### ret

> `readonly` **ret**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:378](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L378)

***

### effects

> `readonly` **effects**: readonly [`Effect`](../../../enumerations/Effect.md)[]

Defined in: [types.ts:379](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L379)

***

### body

> `readonly` **body**: [`Block`](Block.md)

Defined in: [types.ts:380](https://github.com/wontlost-ltd/aster-lang/blob/6af9bdf2ab5ee8acfbe773a46b50d724a13df7f9/src/types.ts#L380)
