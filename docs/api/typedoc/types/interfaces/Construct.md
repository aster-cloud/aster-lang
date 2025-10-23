[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Construct

Defined in: [types.ts:294](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L294)

构造器表达式基础接口。

## Extends

- [`BaseConstruct`](../base/interfaces/BaseConstruct.md)\<[`Span`](Span.md), [`ConstructField`](ConstructField.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:295](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types.ts#L295)

#### Overrides

[`BaseConstruct`](../base/interfaces/BaseConstruct.md).[`span`](../base/interfaces/BaseConstruct.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L35)

#### Inherited from

[`BaseConstruct`](../base/interfaces/BaseConstruct.md).[`origin`](../base/interfaces/BaseConstruct.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L36)

#### Inherited from

[`BaseConstruct`](../base/interfaces/BaseConstruct.md).[`file`](../base/interfaces/BaseConstruct.md#file)

***

### kind

> `readonly` **kind**: `"Construct"`

Defined in: [types/base.ts:327](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L327)

#### Inherited from

[`Construct`](../namespaces/Core/interfaces/Construct.md).[`kind`](../namespaces/Core/interfaces/Construct.md#kind)

***

### typeName

> `readonly` **typeName**: `string`

Defined in: [types/base.ts:328](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L328)

#### Inherited from

[`Construct`](../namespaces/Core/interfaces/Construct.md).[`typeName`](../namespaces/Core/interfaces/Construct.md#typename)

***

### fields

> `readonly` **fields**: readonly [`ConstructField`](ConstructField.md)[]

Defined in: [types/base.ts:329](https://github.com/wontlost-ltd/aster-lang/blob/9d401c3f80bd10cde66ddfef400020ddf8f12a80/src/types/base.ts#L329)

#### Inherited from

[`BaseConstruct`](../base/interfaces/BaseConstruct.md).[`fields`](../base/interfaces/BaseConstruct.md#fields)
