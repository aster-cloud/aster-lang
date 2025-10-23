[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseInt\<S\>

Defined in: [types/base.ts:265](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L265)

整数字面量基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Int`](../../interfaces/Int.md)
- [`Int`](../../namespaces/Core/interfaces/Int.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### span?

> `readonly` `optional` **span**: `HasFileProp`\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: `HasFileProp`\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L35)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L36)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Int"`

Defined in: [types/base.ts:266](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L266)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### value

> `readonly` **value**: `number`

Defined in: [types/base.ts:267](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L267)
