[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Module

Defined in: [types.ts:139](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types.ts#L139)

Module 模块基础接口。

## Extends

- [`BaseModule`](../base/interfaces/BaseModule.md)\<[`Span`](Span.md), [`Declaration`](../type-aliases/Declaration.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:140](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types.ts#L140)

#### Overrides

[`BaseModule`](../base/interfaces/BaseModule.md).[`span`](../base/interfaces/BaseModule.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L42)

#### Inherited from

[`BaseModule`](../base/interfaces/BaseModule.md).[`origin`](../base/interfaces/BaseModule.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](../base/interfaces/BaseNode.md).[`file`](../base/interfaces/BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"Module"`

Defined in: [types/base.ts:91](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L91)

#### Inherited from

[`Module`](../namespaces/Core/interfaces/Module.md).[`kind`](../namespaces/Core/interfaces/Module.md#kind)

***

### name

> `readonly` **name**: `null` \| `string`

Defined in: [types/base.ts:92](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L92)

#### Inherited from

[`Module`](../namespaces/Core/interfaces/Module.md).[`name`](../namespaces/Core/interfaces/Module.md#name)

***

### decls

> `readonly` **decls**: readonly [`Declaration`](../type-aliases/Declaration.md)[]

Defined in: [types/base.ts:93](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types/base.ts#L93)

#### Inherited from

[`BaseModule`](../base/interfaces/BaseModule.md).[`decls`](../base/interfaces/BaseModule.md#decls)
