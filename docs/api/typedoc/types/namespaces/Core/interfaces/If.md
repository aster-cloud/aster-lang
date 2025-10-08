[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: If

Defined in: [types.ts:267](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types.ts#L267)

## Extends

- `BaseIf`\<[`Origin`](../../../interfaces/Origin.md), [`Expression`](../type-aliases/Expression.md), [`Block`](Block.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L32)

#### Inherited from

`Base.BaseIf.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L33)

#### Inherited from

`Base.BaseIf.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L34)

#### Inherited from

`Base.BaseIf.file`

***

### kind

> `readonly` **kind**: `"If"`

Defined in: [types/base.ts:161](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L161)

#### Inherited from

`Base.BaseIf.kind`

***

### cond

> `readonly` **cond**: [`Expression`](../type-aliases/Expression.md)

Defined in: [types/base.ts:162](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L162)

#### Inherited from

`Base.BaseIf.cond`

***

### thenBlock

> `readonly` **thenBlock**: [`Block`](Block.md)

Defined in: [types/base.ts:163](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L163)

#### Inherited from

`Base.BaseIf.thenBlock`

***

### elseBlock

> `readonly` **elseBlock**: `null` \| [`Block`](Block.md)

Defined in: [types/base.ts:164](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L164)

#### Inherited from

`Base.BaseIf.elseBlock`
