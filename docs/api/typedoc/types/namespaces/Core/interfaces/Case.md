[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Case

Defined in: [types.ts:441](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L441)

## Extends

- [`CoreNode`](CoreNode.md)

## Properties

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types.ts:341](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L341)

#### Inherited from

[`CoreNode`](CoreNode.md).[`origin`](CoreNode.md#origin)

***

### kind

> `readonly` **kind**: `"Case"`

Defined in: [types.ts:442](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L442)

#### Overrides

[`CoreNode`](CoreNode.md).[`kind`](CoreNode.md#kind)

***

### pattern

> `readonly` **pattern**: [`Pattern`](../type-aliases/Pattern.md)

Defined in: [types.ts:443](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L443)

***

### body

> `readonly` **body**: [`Block`](Block.md) \| [`Return`](Return.md)

Defined in: [types.ts:444](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L444)
