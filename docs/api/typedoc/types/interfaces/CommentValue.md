[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: CommentValue

Defined in: [types.ts:110](https://github.com/wontlost-ltd/aster-lang/blob/2b787d0632f54e12e5d73672527a6dad5f0766ce/src/types.ts#L110)

注释 Token 的取值结构

保存原始文本、整理后的主体文本以及注释分类，使词法分析阶段的注释处理更加可控。

## Properties

### raw

> `readonly` **raw**: `string`

Defined in: [types.ts:111](https://github.com/wontlost-ltd/aster-lang/blob/2b787d0632f54e12e5d73672527a6dad5f0766ce/src/types.ts#L111)

***

### text

> `readonly` **text**: `string`

Defined in: [types.ts:112](https://github.com/wontlost-ltd/aster-lang/blob/2b787d0632f54e12e5d73672527a6dad5f0766ce/src/types.ts#L112)

***

### trivia

> `readonly` **trivia**: `"inline"` \| `"standalone"`

Defined in: [types.ts:113](https://github.com/wontlost-ltd/aster-lang/blob/2b787d0632f54e12e5d73672527a6dad5f0766ce/src/types.ts#L113)
