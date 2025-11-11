[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BasePatternCtor\<S, Pat\>

Defined in: [types/base.ts:267](https://github.com/wontlost-ltd/aster-lang/blob/3fcb2e3cbadc78c9b553141ad532d86964542285/src/types/base.ts#L267)

构造器模式基础接口。

## Extends

- [`BaseNode`](BaseNode.md)\<`S`\>

## Extended by

- [`PatternCtor`](../../interfaces/PatternCtor.md)
- [`PatCtor`](../../namespaces/Core/interfaces/PatCtor.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

### Pat

`Pat` = `unknown`

## Properties

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/3fcb2e3cbadc78c9b553141ad532d86964542285/src/types/base.ts#L41)

#### Inherited from

[`BaseNode`](BaseNode.md).[`span`](BaseNode.md#span)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/3fcb2e3cbadc78c9b553141ad532d86964542285/src/types/base.ts#L42)

#### Inherited from

[`BaseNode`](BaseNode.md).[`origin`](BaseNode.md#origin)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/3fcb2e3cbadc78c9b553141ad532d86964542285/src/types/base.ts#L43)

#### Inherited from

[`BaseNode`](BaseNode.md).[`file`](BaseNode.md#file)

***

### kind

> `readonly` **kind**: `"PatternCtor"` \| `"PatCtor"`

Defined in: [types/base.ts:268](https://github.com/wontlost-ltd/aster-lang/blob/3fcb2e3cbadc78c9b553141ad532d86964542285/src/types/base.ts#L268)

#### Overrides

[`BaseNode`](BaseNode.md).[`kind`](BaseNode.md#kind)

***

### typeName

> `readonly` **typeName**: `string`

Defined in: [types/base.ts:269](https://github.com/wontlost-ltd/aster-lang/blob/3fcb2e3cbadc78c9b553141ad532d86964542285/src/types/base.ts#L269)

***

### names

> `readonly` **names**: readonly `string`[]

Defined in: [types/base.ts:270](https://github.com/wontlost-ltd/aster-lang/blob/3fcb2e3cbadc78c9b553141ad532d86964542285/src/types/base.ts#L270)

***

### args?

> `readonly` `optional` **args**: readonly `Pat`[]

Defined in: [types/base.ts:271](https://github.com/wontlost-ltd/aster-lang/blob/3fcb2e3cbadc78c9b553141ad532d86964542285/src/types/base.ts#L271)
