[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Construct

Defined in: [types.ts:208](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types.ts#L208)

构造器表达式基础接口。

## Extends

- [`BaseConstruct`](../base/interfaces/BaseConstruct.md)\<[`Span`](Span.md), [`ConstructField`](ConstructField.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types/base.ts#L32)

#### Inherited from

[`BaseConstruct`](../base/interfaces/BaseConstruct.md).[`span`](../base/interfaces/BaseConstruct.md#span)

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types/base.ts#L33)

#### Inherited from

[`BaseConstruct`](../base/interfaces/BaseConstruct.md).[`origin`](../base/interfaces/BaseConstruct.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types/base.ts#L34)

#### Inherited from

[`BaseConstruct`](../base/interfaces/BaseConstruct.md).[`file`](../base/interfaces/BaseConstruct.md#file)

***

### kind

> `readonly` **kind**: `"Construct"`

Defined in: [types/base.ts:322](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types/base.ts#L322)

#### Inherited from

[`BaseConstruct`](../base/interfaces/BaseConstruct.md).[`kind`](../base/interfaces/BaseConstruct.md#kind)

***

### typeName

> `readonly` **typeName**: `string`

Defined in: [types/base.ts:323](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types/base.ts#L323)

#### Inherited from

[`Construct`](../namespaces/Core/interfaces/Construct.md).[`typeName`](../namespaces/Core/interfaces/Construct.md#typename)

***

### fields

> `readonly` **fields**: readonly [`ConstructField`](ConstructField.md)[]

Defined in: [types/base.ts:324](https://github.com/wontlost-ltd/aster-lang/blob/1faf303c4ce91a4234e070e405f7ddbcc6e172c4/src/types/base.ts#L324)

#### Inherited from

[`BaseConstruct`](../base/interfaces/BaseConstruct.md).[`fields`](../base/interfaces/BaseConstruct.md#fields)
