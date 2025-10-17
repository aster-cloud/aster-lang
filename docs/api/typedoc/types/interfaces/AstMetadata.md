[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: AstMetadata

Defined in: [types.ts:28](https://github.com/wontlost-ltd/aster-lang/blob/04ee1d5c525ac87c27117626540a0426a29298e4/src/types.ts#L28)

AST 节点的基础元数据接口

用于为 AST 节点附加位置、来源等元数据信息，消除 `(x as any).span = ...` 模式。

## Properties

### span?

> `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:30](https://github.com/wontlost-ltd/aster-lang/blob/04ee1d5c525ac87c27117626540a0426a29298e4/src/types.ts#L30)

源代码位置信息（可选，由 parser 附加）

***

### origin?

> `optional` **origin**: [`Origin`](Origin.md)

Defined in: [types.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/04ee1d5c525ac87c27117626540a0426a29298e4/src/types.ts#L32)

来源文件信息（可选，由 lower_to_core 附加）

***

### file?

> `optional` **file**: `null` \| `string`

Defined in: [types.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/04ee1d5c525ac87c27117626540a0426a29298e4/src/types.ts#L34)

文件路径（可选）
