[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: If

Defined in: [types.ts:428](https://github.com/wontlost-ltd/aster-lang/blob/f3be429a3dd4fa3b3fa1ca69bb6ae12cde8a35f5/src/types.ts#L428)

## Extends

- [`CoreNode`](CoreNode.md)

## Properties

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types.ts:341](https://github.com/wontlost-ltd/aster-lang/blob/f3be429a3dd4fa3b3fa1ca69bb6ae12cde8a35f5/src/types.ts#L341)

#### Inherited from

[`CoreNode`](CoreNode.md).[`origin`](CoreNode.md#origin)

***

### kind

> `readonly` **kind**: `"If"`

Defined in: [types.ts:429](https://github.com/wontlost-ltd/aster-lang/blob/f3be429a3dd4fa3b3fa1ca69bb6ae12cde8a35f5/src/types.ts#L429)

#### Overrides

[`CoreNode`](CoreNode.md).[`kind`](CoreNode.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:430](https://github.com/wontlost-ltd/aster-lang/blob/f3be429a3dd4fa3b3fa1ca69bb6ae12cde8a35f5/src/types.ts#L430)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types.ts:431](https://github.com/wontlost-ltd/aster-lang/blob/f3be429a3dd4fa3b3fa1ca69bb6ae12cde8a35f5/src/types.ts#L431)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:432](https://github.com/wontlost-ltd/aster-lang/blob/f3be429a3dd4fa3b3fa1ca69bb6ae12cde8a35f5/src/types.ts#L432)
