[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Construct

Defined in: [types.ts:322](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types.ts#L322)

构造器表达式基础接口。

## Extends

- [`BaseConstruct`](../base/interfaces/BaseConstruct.md)\<[`Span`](Span.md), [`ConstructField`](ConstructField.md)\>

## Properties

### span

> **span**: [`Span`](Span.md)

Defined in: [types.ts:323](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types.ts#L323)

#### Overrides

[`BaseConstruct`](../base/interfaces/BaseConstruct.md).[`span`](../base/interfaces/BaseConstruct.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L42)

#### Inherited from

[`BaseConstruct`](../base/interfaces/BaseConstruct.md).[`origin`](../base/interfaces/BaseConstruct.md#origin)

***

### file?

> `readonly` `optional` **file**: `string` \| `null`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L43)

#### Inherited from

[`BaseConstruct`](../base/interfaces/BaseConstruct.md).[`file`](../base/interfaces/BaseConstruct.md#file)

***

### kind

> `readonly` **kind**: `"Construct"`

Defined in: [types/base.ts:375](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L375)

#### Inherited from

[`Construct`](../namespaces/Core/interfaces/Construct.md).[`kind`](../namespaces/Core/interfaces/Construct.md#kind)

***

### typeName

> `readonly` **typeName**: `string`

Defined in: [types/base.ts:376](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L376)

#### Inherited from

[`Construct`](../namespaces/Core/interfaces/Construct.md).[`typeName`](../namespaces/Core/interfaces/Construct.md#typename)

***

### fields

> `readonly` **fields**: readonly [`ConstructField`](ConstructField.md)[]

Defined in: [types/base.ts:377](https://github.com/wontlost-ltd/aster-lang/blob/f2c88b0f9656f97c1d35afd95f0da3a64883238f/src/types/base.ts#L377)

#### Inherited from

[`BaseConstruct`](../base/interfaces/BaseConstruct.md).[`fields`](../base/interfaces/BaseConstruct.md#fields)
