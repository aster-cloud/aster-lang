[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: EffectCapable

Defined in: [types.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/a9e060fb41d6a294aa63fe17f1f1a4eebbf9d535/src/types.ts#L43)

带有效应能力标注的 AST 节点接口

用于在语法分析阶段附加效应能力信息（如 `[files, secrets]`），
支持细粒度的效应跟踪和验证。

## Properties

### effectCaps?

> `optional` **effectCaps**: readonly [`CapabilityKind`](../type-aliases/CapabilityKind.md)[]

Defined in: [types.ts:45](https://github.com/wontlost-ltd/aster-lang/blob/a9e060fb41d6a294aa63fe17f1f1a4eebbf9d535/src/types.ts#L45)

效应能力列表（可选，由 parser 附加）

***

### effectCapsExplicit?

> `optional` **effectCapsExplicit**: `boolean`

Defined in: [types.ts:47](https://github.com/wontlost-ltd/aster-lang/blob/a9e060fb41d6a294aa63fe17f1f1a4eebbf9d535/src/types.ts#L47)

效应能力是否显式声明（区分隐式推导和显式标注）
