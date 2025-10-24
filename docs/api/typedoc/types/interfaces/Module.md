[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Module

Defined in: [types.ts:139](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L139)

Module 模块基础接口。

## Extends

- [`BaseModule`](../base/interfaces/BaseModule.md)\<[`Span`](Span.md), [`Declaration`](../type-aliases/Declaration.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:140](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L140)

#### Overrides

[`BaseModule`](../base/interfaces/BaseModule.md).[`span`](../base/interfaces/BaseModule.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L35)

#### Inherited from

[`BaseModule`](../base/interfaces/BaseModule.md).[`origin`](../base/interfaces/BaseModule.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L36)

#### Inherited from

[`BaseNode`](../base/interfaces/BaseNode.md).[`file`](../base/interfaces/BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Module"`

Defined in: [types/base.ts:84](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L84)

#### Inherited from

[`Module`](../namespaces/Core/interfaces/Module.md).[`kind`](../namespaces/Core/interfaces/Module.md#kind)

***

### name

> `readonly` **name**: `null` \| `string`

Defined in: [types/base.ts:85](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L85)

#### Inherited from

[`Module`](../namespaces/Core/interfaces/Module.md).[`name`](../namespaces/Core/interfaces/Module.md#name)

***

### decls

> `readonly` **decls**: readonly [`Declaration`](../type-aliases/Declaration.md)[]

Defined in: [types/base.ts:86](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types/base.ts#L86)

#### Inherited from

[`BaseModule`](../base/interfaces/BaseModule.md).[`decls`](../base/interfaces/BaseModule.md#decls)
