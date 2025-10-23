[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: CommentValue

Defined in: [types.ts:107](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types.ts#L107)

注释 Token 的取值结构

保存原始文本、整理后的主体文本以及注释分类，使词法分析阶段的注释处理更加可控。

## Properties

### raw

> `readonly` **raw**: `string`

Defined in: [types.ts:108](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types.ts#L108)

***

### text

> `readonly` **text**: `string`

Defined in: [types.ts:109](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types.ts#L109)

***

### trivia

> `readonly` **trivia**: `"inline"` \| `"standalone"`

Defined in: [types.ts:110](https://github.com/wontlost-ltd/aster-lang/blob/6475e49b97ba6dba5931ad0c705e0cf6865a0b29/src/types.ts#L110)
