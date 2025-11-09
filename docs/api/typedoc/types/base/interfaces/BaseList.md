[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseList\<S, T\>

Defined in: [types/base.ts:443](https://github.com/wontlost-ltd/aster-lang/blob/efb0f2facb9ed55abb63daeedbecff4695d98be4/src/types/base.ts#L443)

List 类型基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`List`](../../interfaces/List.md)
- [`List`](../../namespaces/Core/interfaces/List.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/efb0f2facb9ed55abb63daeedbecff4695d98be4/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/efb0f2facb9ed55abb63daeedbecff4695d98be4/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/efb0f2facb9ed55abb63daeedbecff4695d98be4/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"List"`

Defined in: [types/base.ts:444](https://github.com/wontlost-ltd/aster-lang/blob/efb0f2facb9ed55abb63daeedbecff4695d98be4/src/types/base.ts#L444)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### type

> `readonly` **type**: `T`

Defined in: [types/base.ts:445](https://github.com/wontlost-ltd/aster-lang/blob/efb0f2facb9ed55abb63daeedbecff4695d98be4/src/types/base.ts#L445)
