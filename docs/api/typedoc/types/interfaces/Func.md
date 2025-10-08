[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: Func

Defined in: [types.ts:85](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types.ts#L85)

## Extends

- `BaseFunc`\<[`Span`](Span.md), readonly `string`[], [`Type`](../type-aliases/Type.md)\>

## Properties

### retType

> `readonly` **retType**: [`Type`](../type-aliases/Type.md)

Defined in: [types.ts:86](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types.ts#L86)

***

### body

> `readonly` **body**: `null` \| [`Block`](Block.md)

Defined in: [types.ts:87](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types.ts#L87)

***

### span?

> `readonly` `optional` **span**: `undefined`

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L32)

#### Inherited from

`Base.BaseFunc.span`

***

### origin?

> `readonly` `optional` **origin**: `undefined`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L33)

#### Inherited from

`Base.BaseFunc.origin`

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L34)

#### Inherited from

`Base.BaseFunc.file`

***

### kind

> `readonly` **kind**: `"Func"`

Defined in: [types/base.ts:95](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L95)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`kind`](../namespaces/Core/interfaces/Func.md#kind)

***

### name

> `readonly` **name**: `string`

Defined in: [types/base.ts:96](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L96)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`name`](../namespaces/Core/interfaces/Func.md#name)

***

### typeParams

> `readonly` **typeParams**: readonly `string`[]

Defined in: [types/base.ts:97](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L97)

#### Inherited from

[`Func`](../namespaces/Core/interfaces/Func.md).[`typeParams`](../namespaces/Core/interfaces/Func.md#typeparams)

***

### params

> `readonly` **params**: readonly `BaseParameter`\<[`Type`](../type-aliases/Type.md)\>[]

Defined in: [types/base.ts:98](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L98)

#### Inherited from

`Base.BaseFunc.params`

***

### effects?

> `readonly` `optional` **effects**: readonly `string`[]

Defined in: [types/base.ts:99](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L99)

#### Inherited from

`Base.BaseFunc.effects`

***

### effectCaps?

> `readonly` `optional` **effectCaps**: `object`

Defined in: [types/base.ts:100](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/types/base.ts#L100)

#### io?

> `readonly` `optional` **io**: readonly `string`[]

#### Inherited from

`Base.BaseFunc.effectCaps`
