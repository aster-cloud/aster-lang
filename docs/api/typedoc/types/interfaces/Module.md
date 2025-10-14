[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Module

Defined in: [types.ts:120](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types.ts#L120)

Module 模块基础接口。

## Extends

- [`BaseModule`](../base/interfaces/BaseModule.md)\<[`Span`](Span.md), [`Declaration`](../type-aliases/Declaration.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types/base.ts#L32)

#### Inherited from

[`BaseModule`](../base/interfaces/BaseModule.md).[`span`](../base/interfaces/BaseModule.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types/base.ts#L33)

#### Inherited from

[`BaseModule`](../base/interfaces/BaseModule.md).[`origin`](../base/interfaces/BaseModule.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types/base.ts#L34)

#### Inherited from

[`BaseNode`](../base/interfaces/BaseNode.md).[`file`](../base/interfaces/BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Module"`

Defined in: [types/base.ts:82](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types/base.ts#L82)

#### Inherited from

[`BaseModule`](../base/interfaces/BaseModule.md).[`kind`](../base/interfaces/BaseModule.md#kind)

***

### name

> `readonly` **name**: `null` \| `string`

Defined in: [types/base.ts:83](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types/base.ts#L83)

#### Inherited from

[`Module`](../namespaces/Core/interfaces/Module.md).[`name`](../namespaces/Core/interfaces/Module.md#name)

***

### decls

> `readonly` **decls**: readonly [`Declaration`](../type-aliases/Declaration.md)[]

Defined in: [types/base.ts:84](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types/base.ts#L84)

#### Inherited from

[`BaseModule`](../base/interfaces/BaseModule.md).[`decls`](../base/interfaces/BaseModule.md#decls)
