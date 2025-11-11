[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseOk\<S, Expr\>

Defined in: [types/base.ts:391](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L391)

Ok 表达式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Ok`](../../interfaces/Ok.md)
- [`Ok`](../../namespaces/Core/interfaces/Ok.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Expr

`Expr` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Ok"`

Defined in: [types/base.ts:392](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L392)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### expr

> `readonly` **expr**: `Expr`

Defined in: [types/base.ts:393](https://github.com/wontlost-ltd/aster-lang/blob/b644d6c321624e121720bf50b6e25f7b32263b9d/src/types/base.ts#L393)
