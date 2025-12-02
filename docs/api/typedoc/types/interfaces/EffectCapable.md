[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: EffectCapable

Defined in: [types.ts:44](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types.ts#L44)

带有效应能力标注的 AST 节点接口

用于在语法分析阶段附加效应能力信息（如 `[files, secrets]`），
支持细粒度的效应跟踪和验证。

## Properties

### effectCaps

> **effectCaps**: [`EffectCaps`](../type-aliases/EffectCaps.md)

Defined in: [types.ts:46](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types.ts#L46)

效应能力列表（无副作用时为空列表）

***

### effectCapsExplicit

> **effectCapsExplicit**: `boolean`

Defined in: [types.ts:48](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types.ts#L48)

效应能力是否显式声明（区分隐式推导和显式标注）
