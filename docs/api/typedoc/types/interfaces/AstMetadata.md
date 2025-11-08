[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: AstMetadata

Defined in: [types.ts:29](https://github.com/wontlost-ltd/aster-lang/blob/4899d627882a090d7d5d1cdf62f9eb9449c10cfb/src/types.ts#L29)

AST 节点的基础元数据接口

用于为 AST 节点附加位置、来源等元数据信息，消除 `(x as any).span = ...` 模式。

## Properties

### span?

> `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:31](https://github.com/wontlost-ltd/aster-lang/blob/4899d627882a090d7d5d1cdf62f9eb9449c10cfb/src/types.ts#L31)

源代码位置信息（可选，由 parser 附加）

***

### origin?

> `optional` **origin**: [`Origin`](Origin.md)

Defined in: [types.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/4899d627882a090d7d5d1cdf62f9eb9449c10cfb/src/types.ts#L33)

来源文件信息（可选，由 lower_to_core 附加）

***

### file?

> `optional` **file**: `null` \| `string`

Defined in: [types.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/4899d627882a090d7d5d1cdf62f9eb9449c10cfb/src/types.ts#L35)

文件路径（可选）
