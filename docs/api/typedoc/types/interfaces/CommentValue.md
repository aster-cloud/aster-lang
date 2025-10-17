[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: CommentValue

Defined in: [types.ts:113](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L113)

注释 Token 的取值结构

保存原始文本、整理后的主体文本以及注释分类，使词法分析阶段的注释处理更加可控。

## Properties

### raw

> `readonly` **raw**: `string`

Defined in: [types.ts:114](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L114)

***

### text

> `readonly` **text**: `string`

Defined in: [types.ts:115](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L115)

***

### trivia

> `readonly` **trivia**: `"inline"` \| `"standalone"`

Defined in: [types.ts:116](https://github.com/wontlost-ltd/aster-lang/blob/e6ea1da7461abaae32be581f99e8f0b907bd3134/src/types.ts#L116)
