[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseMap\<S, T\>

Defined in: [types/base.ts:444](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L444)

Map 类型基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`Map`](../../interfaces/Map.md)
- [`Map`](../../namespaces/Core/interfaces/Map.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### T

`T` = `unknown`

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

> `readonly` **kind**: `"Map"`

Defined in: [types/base.ts:445](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L445)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### key

> `readonly` **key**: `T`

Defined in: [types/base.ts:446](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L446)

***

### val

> `readonly` **val**: `T`

Defined in: [types/base.ts:447](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types/base.ts#L447)
