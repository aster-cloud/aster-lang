[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Scope

Defined in: [types.ts:302](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types.ts#L302)

Scope 作用域基础接口。

## Extends

- [`BaseScope`](../../../base/interfaces/BaseScope.md)\<[`Origin`](../../../interfaces/Origin.md), [`Statement`](../type-aliases/Statement.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L32)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`span`](../../../base/interfaces/BaseScope.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L33)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`origin`](../../../base/interfaces/BaseScope.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L34)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`file`](../../../base/interfaces/BaseScope.md#file)

***

### kind

> `readonly` **kind**: `"Scope"`

Defined in: [types/base.ts:154](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L154)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`kind`](../../../base/interfaces/BaseScope.md#kind)

***

### statements

> `readonly` **statements**: readonly [`Statement`](../type-aliases/Statement.md)[]

Defined in: [types/base.ts:155](https://github.com/wontlost-ltd/aster-lang/blob/96c54c7ace0132c27410501c1363e1a48042e36f/src/types/base.ts#L155)

#### Inherited from

[`BaseScope`](../../../base/interfaces/BaseScope.md).[`statements`](../../../base/interfaces/BaseScope.md#statements)
