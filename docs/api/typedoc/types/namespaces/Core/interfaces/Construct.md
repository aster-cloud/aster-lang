[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: Construct

Defined in: [types.ts:545](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types.ts#L545)

构造器表达式基础接口。

## Extends

- [`BaseConstruct`](../../../base/interfaces/BaseConstruct.md)\<[`Origin`](../../../interfaces/Origin.md), [`ConstructField`](ConstructField.md)\>

## Properties

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types/base.ts#L41)

#### Inherited from

[`BaseConstruct`](../../../base/interfaces/BaseConstruct.md).[`span`](../../../base/interfaces/BaseConstruct.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`Origin`](../../../interfaces/Origin.md)

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types/base.ts#L42)

#### Inherited from

[`BaseConstruct`](../../../base/interfaces/BaseConstruct.md).[`origin`](../../../base/interfaces/BaseConstruct.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types/base.ts#L43)

#### Inherited from

[`BaseConstruct`](../../../base/interfaces/BaseConstruct.md).[`file`](../../../base/interfaces/BaseConstruct.md#file)

***

### kind

> `readonly` **kind**: `"Construct"`

Defined in: [types/base.ts:375](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types/base.ts#L375)

#### Inherited from

[`BaseConstruct`](../../../base/interfaces/BaseConstruct.md).[`kind`](../../../base/interfaces/BaseConstruct.md#kind)

***

### typeName

> `readonly` **typeName**: `string`

Defined in: [types/base.ts:376](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types/base.ts#L376)

#### Inherited from

[`BaseConstruct`](../../../base/interfaces/BaseConstruct.md).[`typeName`](../../../base/interfaces/BaseConstruct.md#typename)

***

### fields

> `readonly` **fields**: readonly [`ConstructField`](ConstructField.md)[]

Defined in: [types/base.ts:377](https://github.com/wontlost-ltd/aster-lang/blob/a67c36a8e84e7e240a01c999dba43474d503b48b/src/types/base.ts#L377)

#### Inherited from

[`BaseConstruct`](../../../base/interfaces/BaseConstruct.md).[`fields`](../../../base/interfaces/BaseConstruct.md#fields)
