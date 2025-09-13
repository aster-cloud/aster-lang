[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: If

Defined in: [types.ts:123](https://github.com/wontlost-ltd/aster-lang/blob/feafe98162fff6418df5dcac7e36eb9617e507f1/src/types.ts#L123)

## Extends

- [`AstNode`](AstNode.md)

## Properties

### kind

> `readonly` **kind**: `"If"`

Defined in: [types.ts:124](https://github.com/wontlost-ltd/aster-lang/blob/feafe98162fff6418df5dcac7e36eb9617e507f1/src/types.ts#L124)

#### Overrides

[`AstNode`](AstNode.md).[`kind`](AstNode.md#kind)

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types.ts:125](https://github.com/wontlost-ltd/aster-lang/blob/feafe98162fff6418df5dcac7e36eb9617e507f1/src/types.ts#L125)

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types.ts:126](https://github.com/wontlost-ltd/aster-lang/blob/feafe98162fff6418df5dcac7e36eb9617e507f1/src/types.ts#L126)

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:127](https://github.com/wontlost-ltd/aster-lang/blob/feafe98162fff6418df5dcac7e36eb9617e507f1/src/types.ts#L127)
